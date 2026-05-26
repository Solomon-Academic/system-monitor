package com.monitor.server.ui;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GridLayout;

/**
 * Login dialog for server GUI authentication.
 * Simple username/password validation for access control.
 */
public class LoginDialog extends JDialog {
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private boolean authenticated = false;

    /**
     * Constructs login dialog.
     * @param parent Parent frame
     */
    public LoginDialog(JFrame parent) {
        super(parent, "Login", true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(300, 150);
        setLocationRelativeTo(parent);

        // Create form components
        usernameField = new JTextField(15);
        passwordField = new JPasswordField(15);

        JButton loginButton = new JButton("Login");
        JButton cancelButton = new JButton("Cancel");

        // Create layout
        setLayout(new GridLayout(3, 2, 10, 10));
        add(new JLabel("Username:"));
        add(usernameField);
        add(new JLabel("Password:"));
        add(passwordField);
        add(loginButton);
        add(cancelButton);

        // Handle login action
        loginButton.addActionListener(e -> handleLogin());
        cancelButton.addActionListener(e -> dispose());

        // Allow Enter key for login
        passwordField.addActionListener(e -> handleLogin());
    }

    /**
     * Handles login button click.
     * Validates credentials (default: admin/admin).
     */
    private void handleLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        // Simple hardcoded authentication
        // In production, use proper authentication mechanism
        if ("admin".equals(username) && "admin".equals(password)) {
            authenticated = true;
            dispose();
        } else {
            usernameField.setText("");
            passwordField.setText("");
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Invalid credentials. Try: admin/admin",
                    "Authentication Failed",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Checks if user was successfully authenticated.
     */
    public boolean isAuthenticated() {
        return authenticated;
    }
}
