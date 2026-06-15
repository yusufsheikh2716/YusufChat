/**
 * YusufChat - ChatGUI.java
 * Desktop GUI Application (Java Swing — Dark Theme)
 * Cross-platform: Windows, Mac, Linux
 *
 * Made by Mohd Yusuf | BBD NIIT Lucknow
 *
 * Compile:  javac ChatClient.java ChatGUI.java
 * Run:      java ChatGUI
 * JAR:      jar cfe YusufChat-Desktop.jar ChatGUI ChatClient.class ChatGUI*.class
 *           java -jar YusufChat-Desktop.jar
 */

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import javax.swing.plaf.basic.*;

public class ChatGUI extends JFrame {

    // ─── Palette ──────────────────────────────────────────────────────────────────
    static final Color BG_DARK       = new Color(0x0F0F1A);
    static final Color BG_PANEL      = new Color(0x1A1A2E);
    static final Color BG_INPUT      = new Color(0x16213E);
    static final Color BG_SIDEBAR    = new Color(0x0D0D1F);
    static final Color ACCENT        = new Color(0x6366F1);
    static final Color ACCENT_LIGHT  = new Color(0x818CF8);
    static final Color ACCENT_DARK   = new Color(0x4F46E5);
    static final Color MSG_OWN_BG    = new Color(0x3730A3);
    static final Color MSG_OTHER_BG  = new Color(0x1E1B4B);
    static final Color MSG_SYS_BG    = new Color(0x1F2937);
    static final Color TEXT_PRIMARY  = new Color(0xF1F5F9);
    static final Color TEXT_MUTED    = new Color(0x94A3B8);
    static final Color TEXT_TIME     = new Color(0x64748B);
    static final Color BORDER        = new Color(0x2D2B55);
    static final Color ONLINE_GREEN  = new Color(0x10B981);

    // ─── Fonts ────────────────────────────────────────────────────────────────────
    static Font FONT_REGULAR;
    static Font FONT_BOLD;
    static Font FONT_SMALL;
    static Font FONT_MONO;

    // ─── Components ───────────────────────────────────────────────────────────────
    JPanel         messagesPanel;
    JScrollPane    scrollPane;
    JTextField     inputField;
    JButton        sendButton;
    JLabel         typingLabel;
    JLabel         statusLabel;
    JLabel         onlineCountLabel;
    DefaultListModel<String> userListModel;
    JList<String>  userList;
    JLabel         headerTitle;

    // ─── Network ──────────────────────────────────────────────────────────────────
    ChatClient client;
    String     username;
    String     serverHost;
    int        serverPort;

    // ─── Typing Timer ─────────────────────────────────────────────────────────────
    javax.swing.Timer typingTimer;
    Map<String, javax.swing.Timer> stopTypingTimers = new HashMap<>();

    // ─── Entry Point ──────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        // Load fonts
        try {
            FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 14);
            FONT_BOLD    = new Font("Segoe UI", Font.BOLD,  14);
            FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 12);
            FONT_MONO    = new Font("Consolas", Font.PLAIN, 13);
        } catch (Exception e) {
            FONT_REGULAR = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
            FONT_BOLD    = new Font(Font.SANS_SERIF, Font.BOLD,  14);
            FONT_SMALL   = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
            FONT_MONO    = new Font(Font.MONOSPACED, Font.PLAIN, 13);
        }

        // Force dark title bar on Windows
        System.setProperty("sun.java2d.opengl", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            applyGlobalDarkTheme();
            new ChatGUI().showLoginDialog();
        });
    }

    static void applyGlobalDarkTheme() {
        UIManager.put("Panel.background",              BG_DARK);
        UIManager.put("OptionPane.background",         BG_PANEL);
        UIManager.put("OptionPane.messageForeground",  TEXT_PRIMARY);
        UIManager.put("TextField.background",          BG_INPUT);
        UIManager.put("TextField.foreground",          TEXT_PRIMARY);
        UIManager.put("TextField.caretForeground",     ACCENT_LIGHT);
        UIManager.put("TextField.border",              BorderFactory.createLineBorder(BORDER, 1));
        UIManager.put("Button.background",             ACCENT);
        UIManager.put("Button.foreground",             Color.WHITE);
        UIManager.put("ScrollPane.background",         BG_DARK);
        UIManager.put("ScrollBar.background",          BG_PANEL);
        UIManager.put("ScrollBar.thumb",               BORDER);
        UIManager.put("List.background",               BG_SIDEBAR);
        UIManager.put("List.foreground",               TEXT_PRIMARY);
        UIManager.put("List.selectionBackground",      ACCENT_DARK);
        UIManager.put("List.selectionForeground",      Color.WHITE);
        UIManager.put("Label.foreground",              TEXT_PRIMARY);
        UIManager.put("ComboBox.background",           BG_INPUT);
        UIManager.put("ComboBox.foreground",           TEXT_PRIMARY);
    }

    // ─── Login Dialog ─────────────────────────────────────────────────────────────
    void showLoginDialog() {
        JDialog dialog = new JDialog((Frame) null, "Join YusufChat", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(420, 340);
        dialog.setLocationRelativeTo(null);
        dialog.setResizable(false);

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(0x1A1A2E), 0, getHeight(), new Color(0x0D0D1F));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        // Logo
        JPanel logoPanel = new JPanel();
        logoPanel.setOpaque(false);
        logoPanel.setBorder(new EmptyBorder(24, 0, 8, 0));

        JLabel logoIcon = new JLabel("Y") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 26));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("Y",
                    (getWidth()  - fm.stringWidth("Y")) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        logoIcon.setPreferredSize(new Dimension(56, 56));

        JLabel appName  = new JLabel("YusufChat");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 22));
        appName.setForeground(TEXT_PRIMARY);

        JLabel tagline = new JLabel("Connect Instantly");
        tagline.setFont(FONT_SMALL);
        tagline.setForeground(ACCENT_LIGHT);

        JPanel nameTag = new JPanel();
        nameTag.setOpaque(false);
        nameTag.setLayout(new BoxLayout(nameTag, BoxLayout.Y_AXIS));
        appName.setAlignmentX(Component.LEFT_ALIGNMENT);
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameTag.add(appName);
        nameTag.add(tagline);

        logoPanel.add(logoIcon);
        logoPanel.add(Box.createHorizontalStrut(12));
        logoPanel.add(nameTag);

        // Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(new EmptyBorder(8, 36, 8, 36));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JTextField nameField = createStyledTextField("Your display name");
        JTextField hostField = createStyledTextField("Server host");
        hostField.setText(ChatClient.DEFAULT_HOST);
        JTextField portField = createStyledTextField("Port");
        portField.setText(String.valueOf(ChatClient.DEFAULT_PORT));

        gbc.gridy = 0; formPanel.add(new JLabel("Username") {{ setForeground(TEXT_MUTED); setFont(FONT_SMALL); }}, gbc);
        gbc.gridy = 1; formPanel.add(nameField, gbc);
        gbc.gridy = 2; formPanel.add(new JLabel("Server Host") {{ setForeground(TEXT_MUTED); setFont(FONT_SMALL); }}, gbc);
        gbc.gridy = 3; formPanel.add(hostField, gbc);
        gbc.gridy = 4; formPanel.add(new JLabel("Port") {{ setForeground(TEXT_MUTED); setFont(FONT_SMALL); }}, gbc);
        gbc.gridy = 5; formPanel.add(portField, gbc);

        // Join button
        JButton joinButton = createAccentButton("Join Chat →");

        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);
        btnPanel.setBorder(new EmptyBorder(0, 36, 0, 36));
        btnPanel.setLayout(new BorderLayout());
        btnPanel.add(joinButton, BorderLayout.CENTER);

        // Footer
        JLabel footer = new JLabel("Made by Mohd Yusuf | BBD NIIT Lucknow", SwingConstants.CENTER);
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        footer.setForeground(TEXT_TIME);
        footer.setBorder(new EmptyBorder(12, 0, 12, 0));

        root.add(logoPanel,  BorderLayout.NORTH);
        root.add(formPanel,  BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setOpaque(false);
        southPanel.add(btnPanel, BorderLayout.CENTER);
        southPanel.add(footer,   BorderLayout.SOUTH);
        root.add(southPanel, BorderLayout.SOUTH);

        dialog.setContentPane(root);

        Runnable doJoin = () -> {
            String name = nameField.getText().trim();
            String host = hostField.getText().trim();
            String portStr = portField.getText().trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a username.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            this.username   = name;
            this.serverHost = host.isEmpty()    ? ChatClient.DEFAULT_HOST : host;
            this.serverPort = portStr.isEmpty() ? ChatClient.DEFAULT_PORT :
                              parseInt(portStr, ChatClient.DEFAULT_PORT);

            dialog.dispose();
            buildMainWindow();
            connectToServer();
        };

        joinButton.addActionListener(e -> doJoin.run());
        nameField.addActionListener(e -> doJoin.run());
        portField.addActionListener(e -> doJoin.run());

        dialog.setVisible(true);

        // If dialog closed without joining
        if (username == null) System.exit(0);
    }

    // ─── Main Window ─────────────────────────────────────────────────────────────
    void buildMainWindow() {
        setTitle("YusufChat — " + username);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 680);
        setMinimumSize(new Dimension(700, 480));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (client != null) client.disconnect();
            }
        });

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DARK);

        root.add(buildHeader(),   BorderLayout.NORTH);
        root.add(buildSidebar(),  BorderLayout.EAST);
        root.add(buildChatArea(), BorderLayout.CENTER);
        root.add(buildInputBar(), BorderLayout.SOUTH);

        setContentPane(root);
        setVisible(true);
    }

    // ─── Header ───────────────────────────────────────────────────────────────────
    JPanel buildHeader() {
        JPanel header = new GradientPanel(new Color(0x1A1A2E), new Color(0x0F0F1A), true);
        header.setLayout(new BorderLayout());
        header.setBorder(new CompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(12, 20, 12, 20)
        ));
        header.setPreferredSize(new Dimension(0, 60));

        // Left: Logo + Title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel logoIcon = new JLabel("Y") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("Y",
                    (getWidth()  - fm.stringWidth("Y")) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        logoIcon.setPreferredSize(new Dimension(36, 36));
        logoIcon.setOpaque(false);

        headerTitle = new JLabel("YusufChat");
        headerTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerTitle.setForeground(TEXT_PRIMARY);

        JLabel tagline = new JLabel(" · Connect Instantly");
        tagline.setFont(FONT_SMALL);
        tagline.setForeground(ACCENT_LIGHT);

        left.add(logoIcon);
        left.add(headerTitle);
        left.add(tagline);

        // Right: Status + online count
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        statusLabel = new JLabel("Connecting...");
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(TEXT_MUTED);

        onlineCountLabel = new JLabel("0 online");
        onlineCountLabel.setFont(FONT_SMALL);
        onlineCountLabel.setForeground(ONLINE_GREEN);

        right.add(statusLabel);
        right.add(new JSeparator(SwingConstants.VERTICAL));
        right.add(onlineCountLabel);

        header.add(left,  BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    // ─── Sidebar (Online Users) ───────────────────────────────────────────────────
    JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(BG_SIDEBAR);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER));
        sidebar.setPreferredSize(new Dimension(200, 0));

        JLabel title = new JLabel("  Online Users");
        title.setFont(FONT_BOLD);
        title.setForeground(TEXT_MUTED);
        title.setBorder(new EmptyBorder(14, 4, 10, 0));

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel) {
            @Override
            public ListCellRenderer<? super String> getCellRenderer() {
                return (list, value, index, isSelected, cellHasFocus) -> {
                    JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
                    cell.setBackground(isSelected ? ACCENT_DARK : BG_SIDEBAR);

                    // Green dot
                    JLabel dot = new JLabel("●");
                    dot.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                    dot.setForeground(ONLINE_GREEN);

                    JLabel nameLabel = new JLabel(value);
                    nameLabel.setFont(FONT_REGULAR);
                    nameLabel.setForeground(value.equals(username) ? ACCENT_LIGHT : TEXT_PRIMARY);

                    cell.add(dot);
                    cell.add(nameLabel);
                    return cell;
                };
            }
        };
        userList.setBackground(BG_SIDEBAR);
        userList.setFixedCellHeight(36);

        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setBackground(BG_SIDEBAR);
        userScroll.setBorder(BorderFactory.createEmptyBorder());
        userScroll.getViewport().setBackground(BG_SIDEBAR);
        styleScrollBar(userScroll);

        // Footer credit
        JLabel credit = new JLabel("<html><center>Made by Mohd Yusuf<br>BBD NIIT Lucknow</center></html>", SwingConstants.CENTER);
        credit.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        credit.setForeground(TEXT_TIME);
        credit.setBorder(new EmptyBorder(8, 0, 10, 0));

        sidebar.add(title,      BorderLayout.NORTH);
        sidebar.add(userScroll, BorderLayout.CENTER);
        sidebar.add(credit,     BorderLayout.SOUTH);

        return sidebar;
    }

    // ─── Chat Messages Area ───────────────────────────────────────────────────────
    JPanel buildChatArea() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_DARK);

        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(BG_DARK);
        messagesPanel.setBorder(new EmptyBorder(12, 12, 4, 12));

        scrollPane = new JScrollPane(messagesPanel);
        scrollPane.setBackground(BG_DARK);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(BG_DARK);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        styleScrollBar(scrollPane);

        typingLabel = new JLabel(" ");
        typingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        typingLabel.setForeground(TEXT_MUTED);
        typingLabel.setBorder(new EmptyBorder(2, 16, 2, 16));

        wrapper.add(scrollPane,  BorderLayout.CENTER);
        wrapper.add(typingLabel, BorderLayout.SOUTH);
        return wrapper;
    }

    // ─── Input Bar ────────────────────────────────────────────────────────────────
    JPanel buildInputBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(BG_INPUT);
        bar.setBorder(new CompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
            new EmptyBorder(12, 16, 12, 16)
        ));

        inputField = new JTextField();
        inputField.setBackground(BG_PANEL);
        inputField.setForeground(TEXT_PRIMARY);
        inputField.setCaretColor(ACCENT_LIGHT);
        inputField.setFont(FONT_REGULAR);
        inputField.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            new EmptyBorder(8, 14, 8, 14)
        ));
        inputField.setEnabled(false);

        sendButton = createAccentButton("Send");
        sendButton.setPreferredSize(new Dimension(90, 40));
        sendButton.setEnabled(false);

        bar.add(inputField,  BorderLayout.CENTER);
        bar.add(sendButton,  BorderLayout.EAST);

        // Events
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        // Typing indicator
        inputField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private boolean isTyping = false;

            private void startTyping() {
                if (!isTyping) {
                    isTyping = true;
                    if (client != null) client.sendTyping();
                }
                if (typingTimer != null) typingTimer.stop();
                typingTimer = new javax.swing.Timer(2000, ev -> {
                    isTyping = false;
                    if (client != null) client.sendStopTyping();
                });
                typingTimer.setRepeats(false);
                typingTimer.start();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) { startTyping(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { startTyping(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        });

        return bar;
    }

    // ─── Network ──────────────────────────────────────────────────────────────────
    void connectToServer() {
        client = new ChatClient(serverHost, serverPort);

        client.setOnConnected(() -> SwingUtilities.invokeLater(() -> {
            statusLabel.setText("● Connected");
            statusLabel.setForeground(ONLINE_GREEN);
            inputField.setEnabled(true);
            sendButton.setEnabled(true);
            inputField.requestFocus();
        }));

        client.setOnError(msg -> SwingUtilities.invokeLater(() -> {
            statusLabel.setText("⚠ " + msg);
            statusLabel.setForeground(new Color(0xFBBF24));
            inputField.setEnabled(false);
            sendButton.setEnabled(false);
        }));

        client.setOnMessage(msg -> SwingUtilities.invokeLater(() -> addMessageBubble(msg)));

        client.setOnUserListUpdate(users -> SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (String u : users) userListModel.addElement(u);
            onlineCountLabel.setText(users.size() + " online");
        }));

        client.setOnTypingUpdate(typingUser -> SwingUtilities.invokeLater(() -> {
            typingLabel.setText("  ✏ " + typingUser + " is typing...");

            // Auto-clear after 3s if stopTyping doesn't arrive
            javax.swing.Timer t = stopTypingTimers.get(typingUser);
            if (t != null) t.stop();
            javax.swing.Timer clear = new javax.swing.Timer(3000, e -> {
                String current = typingLabel.getText();
                if (current.contains(typingUser)) typingLabel.setText(" ");
            });
            clear.setRepeats(false);
            clear.start();
            stopTypingTimers.put(typingUser, clear);
        }));

        client.setOnStopTyping(typingUser -> SwingUtilities.invokeLater(() -> {
            javax.swing.Timer t = stopTypingTimers.remove(typingUser);
            if (t != null) t.stop();
            String current = typingLabel.getText();
            if (current.contains(typingUser)) typingLabel.setText(" ");
        }));

        client.connect(username);
    }

    void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || client == null) return;
        client.sendMessage(text);
        inputField.setText("");
        if (typingTimer != null) typingTimer.stop();
        client.sendStopTyping();
    }

    // ─── Message Rendering ────────────────────────────────────────────────────────
    void addMessageBubble(ChatClient.ChatMessage msg) {
        if (msg.type.equals("typing") || msg.type.equals("stopTyping")) return;

        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setOpaque(false);
        bubble.setBorder(new EmptyBorder(4, 4, 4, 4));
        bubble.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        boolean isSystem = msg.type.equals("join") || msg.type.equals("leave") || msg.type.equals("welcome");
        boolean isOwn    = msg.isOwn && !isSystem;

        if (isSystem) {
            // Centered system message
            JPanel inner = new RoundedPanel(20, MSG_SYS_BG);
            inner.setLayout(new BorderLayout());
            inner.setBorder(new EmptyBorder(6, 14, 6, 14));

            JLabel lbl = new JLabel(msg.text, SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            lbl.setForeground(TEXT_MUTED);
            inner.add(lbl, BorderLayout.CENTER);

            JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            row.add(inner);

            bubble.add(row);
        } else {
            // Chat message bubble
            Color bgColor = isOwn ? MSG_OWN_BG : MSG_OTHER_BG;

            if (!isOwn) {
                JLabel user = new JLabel("  " + msg.username);
                user.setFont(new Font("Segoe UI", Font.BOLD, 11));
                user.setForeground(ACCENT_LIGHT);
                user.setAlignmentX(isOwn ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
                bubble.add(user);
            }

            JPanel inner = new RoundedPanel(16, bgColor);
            inner.setLayout(new BorderLayout(8, 0));
            inner.setBorder(new EmptyBorder(9, 14, 9, 14));
            inner.setMaximumSize(new Dimension(600, Integer.MAX_VALUE));

            JTextArea textArea = new JTextArea(msg.text);
            textArea.setFont(FONT_REGULAR);
            textArea.setForeground(TEXT_PRIMARY);
            textArea.setBackground(bgColor);
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(true);
            textArea.setEditable(false);
            textArea.setOpaque(false);
            textArea.setBorder(null);
            textArea.setFocusable(false);

            JLabel timeLabel = new JLabel(msg.time);
            timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            timeLabel.setForeground(TEXT_TIME);
            timeLabel.setVerticalAlignment(SwingConstants.BOTTOM);

            inner.add(textArea,  BorderLayout.CENTER);
            inner.add(timeLabel, BorderLayout.EAST);

            JPanel row = new JPanel(new FlowLayout(isOwn ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            row.add(inner);

            bubble.add(row);
        }

        messagesPanel.add(bubble);
        messagesPanel.add(Box.createVerticalStrut(2));
        messagesPanel.revalidate();

        // Auto-scroll
        SwingUtilities.invokeLater(() -> {
            JScrollBar sb = scrollPane.getVerticalScrollBar();
            sb.setValue(sb.getMaximum());
        });

        // Sound notification for incoming messages
        if (!msg.isOwn && !isSystem) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    // ─── UI Helpers ───────────────────────────────────────────────────────────────

    static JTextField createStyledTextField(String placeholder) {
        JTextField field = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !hasFocus()) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setColor(new Color(0x555577));
                    g2.setFont(getFont().deriveFont(Font.ITALIC));
                    g2.drawString(placeholder, 10, getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
                }
            }
        };
        field.setBackground(BG_INPUT);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(ACCENT_LIGHT);
        field.setFont(FONT_REGULAR);
        field.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
        return field;
    }

    static JButton createAccentButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed()   ? ACCENT_DARK :
                           getModel().isRollover()  ? ACCENT_LIGHT : ACCENT;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                    (getWidth()  - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(200, 42));
        return btn;
    }

    static void styleScrollBar(JScrollPane sp) {
        sp.getVerticalScrollBar().setBackground(BG_PANEL);
        sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = BORDER;
                trackColor = BG_PANEL;
            }
            @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }
        });
    }

    static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }

    // ─── Custom Panels ────────────────────────────────────────────────────────────

    static class RoundedPanel extends JPanel {
        final int radius;
        final Color color;

        RoundedPanel(int radius, Color color) {
            this.radius = radius;
            this.color  = color;
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            super.paintComponent(g);
        }
    }

    static class GradientPanel extends JPanel {
        final Color c1, c2;
        final boolean horizontal;

        GradientPanel(Color c1, Color c2, boolean horizontal) {
            this.c1 = c1; this.c2 = c2; this.horizontal = horizontal;
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setPaint(horizontal
                ? new GradientPaint(0, 0, c1, getWidth(), 0, c2)
                : new GradientPaint(0, 0, c1, 0, getHeight(), c2));
            g2.fillRect(0, 0, getWidth(), getHeight());
            super.paintComponent(g);
        }
    }
}
