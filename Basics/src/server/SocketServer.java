package server;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import javax.swing.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import basics.Debug;
import client.SocketClient;

public class SocketServer extends JFrame {
	int port = 3000;
	MemoryMXBean mbean;
	MemoryUsage beforeHeapMemoryUsage;
	JPanel userPanel;
	SocketServer self;
	public static boolean isRunning = false;
	private List<Room> rooms = new ArrayList<Room>();
	private Room lobby;// here for convenience
	private List<Room> isolatedPrelobbies = new ArrayList<Room>();
	JPanel textArea;
	private Dimension windowSize = new Dimension(400, 400);
	int index;
	private final static String PRELOBBY = "PreLobby";
	protected final static String LOBBY = "Lobby";
	private final static String COMMAND_TRIGGER = "/";
	private final static String DISCONNECT = "disconnect";
	private final static String USERS = "users";
	private final static String UPTIME = "uptime";
	private final static String MEMORY = "memory";
	private final static String TERMINATE = "terminate";
	boolean showUI = true;
	Instant start;

	private void start(int port) {
		mbean = ManagementFactory.getMemoryMXBean();
		beforeHeapMemoryUsage = mbean.getHeapMemoryUsage();
		start = Instant.now();
		this.port = port;
		Debug.log("Waiting for client");
		try (ServerSocket serverSocket = new ServerSocket(port);) {
			isRunning = true;
			// create a lobby on start
			Room.setServer(this);
			lobby = new Room(LOBBY);// , this);
			rooms.add(lobby);
			if (showUI) {
				setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				setPreferredSize(windowSize);
				setTitle("Server Console");
				createPanelRoom();
				showUI();
				self = this;
			}
			while (SocketServer.isRunning) {
				try {
					Socket client = serverSocket.accept();
					Debug.log("Client connecting...");
					// Server thread is the server's representation of the client
					ServerThread thread = new ServerThread(client, lobby);
					thread.start();
					// create a dummy room until we get further client details
					// technically once a user fully joins this lobby will be destroyed
					// but we'll track it in an array so we can attempt to clean it up just in case
					Room prelobby = new Room(PRELOBBY);// , this);
					prelobby.addClient(thread);
					isolatedPrelobbies.add(prelobby);

					Debug.log("Client added to clients pool");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				isRunning = false;
				cleanup();
				Debug.log("closing server socket");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected void cleanupRoom(Room r) {
		isolatedPrelobbies.remove(r);
	}

	private void cleanup() {
		Iterator<Room> rooms = this.rooms.iterator();
		while (rooms.hasNext()) {
			Room r = rooms.next();
			try {
				r.close();
			} catch (Exception e) {
				// it's ok to ignore this one
			}
		}
		Iterator<Room> pl = isolatedPrelobbies.iterator();
		while (pl.hasNext()) {
			Room r = pl.next();
			try {
				r.close();
			} catch (Exception e) {
				// it's ok to ignore this one
			}
		}
		try {
			lobby.close();
		} catch (Exception e) {
			// ok to ignore this too
		}
	}

	protected Room getLobby() {
		return lobby;
	}

	/***
	 * Special helper to join the lobby and close the previous room client was in if
	 * it's marked as Prelobby. Mostly used for prelobby once the server receives
	 * more client details.
	 * 
	 * @param client
	 */
	protected void joinLobby(ServerThread client) {
		Room prelobby = client.getCurrentRoom();
		if (joinRoom(LOBBY, client)) {
			prelobby.removeClient(client);
			Debug.log("Added " + client.getClientName() + " to Lobby; Prelobby should self destruct");
		} else {
			Debug.log("Problem moving " + client.getClientName() + " to lobby");
		}
	}

	/***
	 * Helper function to check if room exists by case insensitive name
	 * 
	 * @param roomName The name of the room to look for
	 * @return matched Room or null if not found
	 */
	private Room getRoom(String roomName) {
		for (int i = 0, l = rooms.size(); i < l; i++) {
			Room r = rooms.get(i);
			if (r == null || r.getName() == null) {
				continue;
			}
			if (r.getName().equalsIgnoreCase(roomName)) {
				return r;
			}
		}
		return null;
	}

	/***
	 * Attempts to join a room by name. Will remove client from old room and add
	 * them to the new room.
	 * 
	 * @param roomName The desired room to join
	 * @param client   The client moving rooms
	 * @return true if reassign worked; false if new room doesn't exist
	 */
	protected synchronized boolean joinRoom(String roomName, ServerThread client) {
		if (roomName == null || roomName.equalsIgnoreCase(PRELOBBY)) {
			return false;
		}
		Room newRoom = getRoom(roomName);
		Room oldRoom = client.getCurrentRoom();
		if (newRoom != null) {
			if (oldRoom != null) {
				Debug.log(client.getClientName() + " leaving room " + oldRoom.getName());
				oldRoom.removeClient(client);
			}
			Debug.log(client.getClientName() + " joining room " + newRoom.getName());
			newRoom.addClient(client);
			return true;
		}
		return false;
	}

	/***
	 * Attempts to create a room with given name if it doesn't exist already.
	 * 
	 * @param roomName The desired room to create
	 * @return true if it was created and false if it exists
	 */
	protected synchronized boolean createNewRoom(String roomName) {
		if (roomName == null || roomName.equalsIgnoreCase(PRELOBBY)) {
			return false;
		}
		if (getRoom(roomName) != null) {
			// TODO can't create room
			Debug.log("Room already exists");
			return false;
		} else {
			Room room = new Room(roomName);// , this);
			rooms.add(room);
			Debug.log("Created new room: " + roomName);
			return true;
		}
	}

	public static void main(String[] args) {
		// let's allow port to be passed as a command line arg
		// in eclipse you can set this via "Run Configurations"
		// -> "Arguments" -> type the port in the text box -> Apply
		int port = -1;
		int showUITemp = 1;
		try {
			port = Integer.parseInt(args[0]);
		} catch (Exception e) {
			// ignore this, we know it was a parsing issue
		}
		try {
			showUITemp = Integer.parseInt(args[1]);
		} catch (Exception e) {
			// ignore this, we know it was a parsing issue
		}
		if (port > -1) {
			Debug.log("Starting Server");
			SocketServer server = new SocketServer();
			if (showUITemp == 0) {
				server.showUI = false;
			}
			else {
				server.showUI = true;
			}
			Debug.log("Listening on port " + port);
			server.start(port);
			Debug.log("Server Stopped");
		}
	}
	
	void createPanelRoom() {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		textArea = new JPanel();
		textArea.setLayout(new BoxLayout(textArea, BoxLayout.Y_AXIS));
		textArea.setAlignmentY(Component.BOTTOM_ALIGNMENT);
		JScrollPane scroll = new JScrollPane(textArea);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		panel.add(scroll, BorderLayout.CENTER);

		JPanel input = new JPanel();
		input.setLayout(new BoxLayout(input, BoxLayout.X_AXIS));
		JTextField text = new JTextField();
		input.add(text);
		JButton button = new JButton("Send");
		text.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "sendAction");
		text.getActionMap().put("sendAction", new AbstractAction() {
			public void actionPerformed(ActionEvent actionEvent) {
				button.doClick();
			}
		});

		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (text.getText().length() > 0) {
					processCommand(text.getText());
					text.setText("");
				}
			}

		});
		input.add(button);
		panel.add(input, BorderLayout.SOUTH);
		this.add(panel);
	}
	
	
	void showUI() {
		pack();
		Dimension lock = textArea.getSize();
		textArea.setMaximumSize(lock);
		setVisible(true);
	}
	
	int calcHeightForText(String str) {
		FontMetrics metrics = self.getGraphics().getFontMetrics(self.getFont());
		int hgt = metrics.getHeight();
		int adv = metrics.stringWidth(str);
		final int PIXEL_PADDING = 6;
		Dimension size = new Dimension(adv, hgt + PIXEL_PADDING);
		final float PADDING_PERCENT = 1.1f;
		// calculate modifier to line wrapping so we can display the wrapped message
		int mult = (int) Math.floor(size.width / (textArea.getSize().width * PADDING_PERCENT));
		// System.out.println(mult);
		mult++;
		return size.height * mult;
	}
	
	public void processCommand(String message) {
		try {
			if (message.indexOf(COMMAND_TRIGGER) > -1) {
				String[] comm = message.split(COMMAND_TRIGGER);

				String part1 = comm[1];
				String[] comm2 = part1.split(" ");
				String command = comm2[0];
				if (command != null) {
					command = command.toLowerCase();
				}
				switch (command) {
				case DISCONNECT:
					message = message.replace("/disconnect ", "");
					Iterator<Room> iter = rooms.iterator();
					while (iter.hasNext()) {
						Room room = iter.next();
						room.disconnectRequest(message);
					}
					message = "/disconnect " + message;
					break;
				case USERS:
					String userString = "";
					processCommand("Users: ");
					processCommand("------------------------------------------");
					Iterator<Room> iter2 = rooms.iterator();
					while (iter2.hasNext()) {
						Room room = iter2.next();
						processCommand(room.getName() + ": " + room.returnUserList());
					}
					message = "------------------------------------------";
					break;
				case UPTIME:
					Instant end = Instant.now();
					Duration uptime = Duration.between(start, end);
					Long l = uptime.toMinutes();
					Long j = (uptime.toMillis() / 1000) % 60;
					message = "Uptime: " + Long.toString(l) + " minutes, " + Long.toString(j) + " seconds";
					break;
				case MEMORY:
					MemoryUsage afterHeapMemoryUsage = mbean.getHeapMemoryUsage();
					long consumed = afterHeapMemoryUsage.getUsed() - 
					                beforeHeapMemoryUsage.getUsed();
					message = ("Estimated consumed Memory: " + consumed + " bytes");
					break;
				case TERMINATE:
					processCommand("Server closing...");
					Iterator<Room> iter3 = rooms.iterator();
					while (iter3.hasNext()) {
						Room room = iter3.next();
						room.closeMessage();
					}
					cleanup();
					System.exit(0);
				}
			}
		 } catch (Exception e) {
			e.printStackTrace();
		}
		JEditorPane entry = new JEditorPane();
		entry.setEditable(false);
		entry.setText(message);
		Dimension d = new Dimension(textArea.getSize().width, calcHeightForText(message));
		// attempt to lock all dimensions
		entry.setMinimumSize(d);
		entry.setPreferredSize(d);
		entry.setMaximumSize(d);
		textArea.add(entry);
		pack();
		System.out.println(entry.getSize());
		JScrollBar sb = ((JScrollPane) textArea.getParent().getParent()).getVerticalScrollBar();
		sb.setValue(sb.getMaximum());
	}
}