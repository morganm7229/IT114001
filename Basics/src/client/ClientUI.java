package client;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;

public class ClientUI extends JFrame implements Event {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	CardLayout card;
	ClientUI self;
	String userToColor;
	JPanel textArea;
	String originalStr;
	JPanel userPanel;
	boolean checkColor = false;
	boolean checkExport = false;
	List<User> users = new ArrayList<User>();
	ArrayList<String> messages = new ArrayList<String>();
	private final static Logger log = Logger.getLogger(ClientUI.class.getName());
	Dimension windowSize = new Dimension(400, 400);

	public ClientUI(String title) {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setPreferredSize(windowSize);
		setLocationRelativeTo(null);
		self = this;
		setTitle(title);
		card = new CardLayout();
		setLayout(card);
		createConnectionScreen();
		createUserInputScreen();
		createPanelRoom();
		createPanelUserList();
		showUI();
	}

	void createConnectionScreen() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		JLabel hostLabel = new JLabel("Host:");
		JTextField host = new JTextField("127.0.0.1");
		panel.add(hostLabel);
		panel.add(host);
		JLabel portLabel = new JLabel("Port:");
		JTextField port = new JTextField("3000");
		panel.add(portLabel);
		panel.add(port);
		JButton button = new JButton("Next");
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String _host = host.getText();
				String _port = port.getText();
				if (_host.length() > 0 && _port.length() > 0) {
					try {
						connect(_host, _port);
						self.next();
					} catch (IOException e1) {
						e1.printStackTrace();
						// TODO handle error properly
						log.log(Level.SEVERE, "Error connecting");
					}
				}
			}

		});
		panel.add(button);
		this.add(panel);
	}

	void createUserInputScreen() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		JLabel userLabel = new JLabel("Username:");
		JTextField username = new JTextField();
		panel.add(userLabel);
		panel.add(username);
		JButton button = new JButton("Join");
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String name = username.getText();
				if (name != null && name.length() > 0) {
					SocketClient.setUsername(name);
					self.next();
				}
			}

		});
		panel.add(button);
		this.add(panel);
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
					SocketClient.sendMessage(text.getText());
					text.setText("");
				}
			}

		});
		input.add(button);
		panel.add(input, BorderLayout.SOUTH);
		this.add(panel);
	}

	void createPanelUserList() {
		userPanel = new JPanel();
		userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.Y_AXIS));
		userPanel.setAlignmentY(Component.TOP_ALIGNMENT);

		JScrollPane scroll = new JScrollPane(userPanel);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

		Dimension d = new Dimension(100, windowSize.height);
		scroll.setPreferredSize(d);

		textArea.getParent().getParent().getParent().add(scroll, BorderLayout.EAST);
	}

	void addClient(String name) {
		User u = new User(name);
		Dimension p = new Dimension(userPanel.getSize().width, 30);
		u.setPreferredSize(p);
		u.setMinimumSize(p);
		u.setMaximumSize(p);
		u.setBackground(Color.white);
		userPanel.add(u);
		users.add(u);
		pack();
	}

	void removeClient(User client) {
		userPanel.remove(client);
		client.removeAll();
		userPanel.revalidate();
		userPanel.repaint();
	}

	/***
	 * Attempts to calculate the necessary dimensions for a potentially wrapped
	 * string of text. This isn't perfect and some extra whitespace above or below
	 * the text may occur
	 * 
	 * @param str
	 * @return
	 */
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

	void addMessage(String str) {
		JEditorPane entry = new JEditorPane();
		entry.setEditable(false);
		// entry.setLayout(null);
		str += " ";
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '&') {
				if (str.charAt(i + 1) == ' ') {
					str = str.replaceFirst("& ", "</b> ");
					entry.setContentType("text/html");
				}
			}
		}
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '&') {
				str = str.replaceFirst("&", "<b>");
				entry.setContentType("text/html");
			}
		}
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '#') {
				if (str.charAt(i + 1) == ' ') {
					str = str.replaceFirst("# ", "</i> ");
					entry.setContentType("text/html");
				}
			}
		}
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '#') {
				str = str.replaceFirst("#", "<i>");
				entry.setContentType("text/html");
			}
		}
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '_') {
				if (str.charAt(i + 1) == ' ') {
					str = str.replaceFirst("_ ", "</u> ");
					entry.setContentType("text/html");
				}
			}
		}
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '_') {
				str = str.replaceFirst("_", "<u>");
				entry.setContentType("text/html");
			}
		}
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '%') {
				if (str.charAt(i + 1) == ' ') {
					str = str.replaceFirst("% ", "</font> ");
					entry.setContentType("text/html");
				}
			}
		}
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '%') {
				str = str.replaceFirst("%", "<font style=color:red>");
				entry.setContentType("text/html");
			}
		}
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '~') {
				originalStr = str.replace("~", "/color ");
				str = str.replace("~", "");
				userToColor = "";
				for (i = i; i < str.length(); i++) {
					if (str.charAt(i) == ' ') {
						System.out.println(userToColor);
						break;
					} else {
						userToColor = userToColor + str.charAt(i);
					}
				}
				str = str.replace(userToColor + " ", "");
				String strTwo = "";
				for (int j = 0; j < str.length(); j++) {
					if (str.charAt(j) == ':') {
						for (int k = j + 2; k < str.length(); k++) {
							if (str.charAt(k) == ' ') {
								break;
							}
							else {
								strTwo = strTwo + str.charAt(k);
							}
						}
					}
				}
				str = originalStr;
				Iterator<User> iter = users.iterator();
				while (iter.hasNext()) {
					User u = iter.next();
					if (u.getName().equals(userToColor)) {
						System.out.println(userToColor);
						System.out.println(strTwo);
						System.out.println("Hi");
						if (strTwo.toLowerCase().equals("blue")) {
							u.setBackground(Color.blue);
							checkColor = true;
						}
						if (strTwo.toLowerCase().equals("black")) {
							u.setBackground(Color.black);
							checkColor = true;
						}
						if (strTwo.toLowerCase().equals("green")) {
							u.setBackground(Color.green);
							checkColor = true;
						}
						if (strTwo.toLowerCase().equals("red")) {
							u.setBackground(Color.red);
							checkColor = true;
						}
						if (strTwo.toLowerCase().equals("yellow")) {
							u.setBackground(Color.yellow);
							checkColor = true;
						}
						if (strTwo.toLowerCase().equals("pink")) {
							u.setBackground(Color.pink);
							checkColor = true;
						}
						if (strTwo.toLowerCase().equals("gray")) {
							u.setBackground(Color.gray);
							checkColor = true;
						}
						if (strTwo.toLowerCase().equals("purple")) {
							u.setBackground(Color.magenta);
							checkColor = true;
						}
						if (strTwo.toLowerCase().equals("orange")) {
							u.setBackground(Color.orange);
							checkColor = true;
						}
						if (strTwo.toLowerCase().equals("white")) {
							u.setBackground(Color.white);
							checkColor = true;
						}
						if (strTwo.toLowerCase().equals("cyan")) {
							u.setBackground(Color.CYAN);
							checkColor = true;
						}
						if (strTwo.toLowerCase().equals("lightgray")) {
							u.setBackground(Color.LIGHT_GRAY);
							checkColor = true;
						}
					}
				}
			}
			if (str.charAt(i) == '`') {
				checkExport = true;
				String exportFileName = "";
				str = str.replace("`", "");
				exportFileName = "";
				for (i = i; i < str.length(); i++) {
					if (str.charAt(i) == ' ') {
						System.out.println(exportFileName);
						break;
					} else {
						exportFileName = exportFileName + str.charAt(i);
					}
				}
				exportText(exportFileName);
			}
		}
		entry.setText(str);
		Dimension d = new Dimension(textArea.getSize().width, calcHeightForText(str));
		// attempt to lock all dimensions
		entry.setMinimumSize(d);
		entry.setPreferredSize(d);
		entry.setMaximumSize(d);
		if (!checkColor && !checkExport) {
			textArea.add(entry);
			messages.add(str);
		}
		checkColor = false;
		checkExport = false;
		pack();
		System.out.println(entry.getSize());
		JScrollBar sb = ((JScrollPane) textArea.getParent().getParent()).getVerticalScrollBar();
		sb.setValue(sb.getMaximum());
	}

	void next() {
		card.next(this.getContentPane());
	}

	void previous() {
		card.previous(this.getContentPane());
	}

	void connect(String host, String port) throws IOException {
		SocketClient.callbackListener(this);
		SocketClient.connectAndStart(host, port);
	}

	void showUI() {
		pack();
		Dimension lock = textArea.getSize();
		textArea.setMaximumSize(lock);
		lock = userPanel.getSize();
		userPanel.setMaximumSize(lock);
		setVisible(true);
	}

	@Override
	public void onClientConnect(String clientName, String message) {
		log.log(Level.INFO, String.format("%s: %s", clientName, message));
		addClient(clientName);
		if (message != null && !message.isEmpty()) {
			self.addMessage(String.format("%s: %s", clientName, message));
		}
	}

	@Override
	public void onClientDisconnect(String clientName, String message) {
		log.log(Level.INFO, String.format("%s: %s", clientName, message));
		Iterator<User> iter = users.iterator();
		while (iter.hasNext()) {
			User u = iter.next();
			if (u.getName() == clientName) {
				removeClient(u);
				iter.remove();
				self.addMessage(String.format("%s: %s", clientName, message));
				break;
			}

		}
	}

	@Override
	public void onMessageReceive(String clientName, String message) {
		log.log(Level.INFO, String.format("%s: %s", clientName, message));
		self.addMessage(String.format("%s: %s", clientName, message));
	}

	@Override
	public void onChangeRoom() {
		Iterator<User> iter = users.iterator();
		while (iter.hasNext()) {
			User u = iter.next();
			removeClient(u);
			iter.remove();
		}
	}

	public static void main(String[] args) {
		ClientUI ui = new ClientUI("My UI");
		if (ui != null) {
			log.log(Level.FINE, "Started");
		}
	}

	public String processMessage(String str) {

		return str;
	}
	
	public void exportText(String fileName) {
		File file = new File(fileName);
		try (FileWriter fw = new FileWriter(fileName + ".txt")) {
			for (int i = 0; i < messages.size(); i++) {
				fw.write(messages.get(i) + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}