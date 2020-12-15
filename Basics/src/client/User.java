package client;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class User extends JPanel {
	private String name;
	private JLabel nameField;
	private String username;

	public User(String name) {
		this.name = name;
		this.username = name;
		nameField = new JLabel(name);
		// nameField.setEditable(false);
		this.setLayout(new BorderLayout());
		this.add(nameField);
	}

	public String getName() {
		return name;
	}

}