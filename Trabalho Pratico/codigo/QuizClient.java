import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;

public class QuizClient extends JFrame {
    private static final int TCP_PORT = 5000;
    private static final int UDP_PORT = 6000;

    private Socket tcpSocket;
    private BufferedReader tcpIn;
    private PrintWriter tcpOut;
    private DatagramSocket udpSocket;

    private String serverIP;
    private String playerName;
    private boolean connected = false;

    // GUI Components
    private JTextField serverIPField;
    private JTextField playerNameField;
    private JButton connectButton;
    private JLabel statusLabel;
    private JLabel timerLabel;
    private JLabel questionLabel;
    private JButton[] answerButtons;
    private JTextArea scoreboardArea;
    private JPanel gamePanel;
    private JPanel connectionPanel;

    // Game state
    private String currentQuestion = "";
    private String[] currentOptions = new String[4];
    private boolean canAnswer = false;

    public QuizClient() {
        setupGUI();
        setupUDPListener();
        System.out.println("setting up udp");
    }

    private void setupGUI() {
        setTitle("Quiz Client - Estilo Kahoot");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new CardLayout());

        setupConnectionPanel();
        setupGamePanel();

        add(connectionPanel, "CONNECTION");
        add(gamePanel, "GAME");

        showConnectionPanel();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void setupConnectionPanel() {
        connectionPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        connectionPanel.setBackground(new Color(46, 125, 50));

        JLabel titleLabel = new JLabel("Quiz Competitivo");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);

        JLabel ipLabel = new JLabel("IP do Servidor:");
        ipLabel.setForeground(Color.WHITE);
        serverIPField = new JTextField("10.0.0.10", 15); // IP padrão da rede R2

        JLabel nameLabel = new JLabel("Seu Nome:");
        nameLabel.setForeground(Color.WHITE);
        playerNameField = new JTextField(15);

        connectButton = new JButton("Conectar");
        connectButton.setBackground(new Color(76, 175, 80));
        connectButton.setForeground(Color.WHITE);
        connectButton.addActionListener(e -> connectToServer());

        statusLabel = new JLabel("Digite o IP do servidor e seu nome");
        statusLabel.setForeground(Color.WHITE);

        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        connectionPanel.add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 1;
        connectionPanel.add(ipLabel, gbc);
        gbc.gridx = 1;
        connectionPanel.add(serverIPField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        connectionPanel.add(nameLabel, gbc);
        gbc.gridx = 1;
        connectionPanel.add(playerNameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        connectionPanel.add(connectButton, gbc);

        gbc.gridy = 4;
        connectionPanel.add(statusLabel, gbc);
    }

    private void setupGamePanel() {
        gamePanel = new JPanel(new BorderLayout());
        gamePanel.setBackground(new Color(33, 150, 243)); // Fundo principal azul claro

        // ==== TOPO ====
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topPanel.setBackground(new Color(25, 118, 210)); // Azul escuro para contraste

       //S timerLabel = new JLabel("Tempo: ");
       // timerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        //timerLabel.setForeground(Color.WHITE);
        //timerLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        //topPanel.add(timerLabel);

        // ==== PERGUNTA E RESPOSTAS ====
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(new Color(33, 150, 243));

        questionLabel = new JLabel("<html><div style='text-align: center;'>Aguardando pergunta...</div></html>");
        questionLabel.setFont(new Font("Arial", Font.BOLD, 18));
        questionLabel.setForeground(Color.WHITE);
        questionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        questionLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        questionLabel.setOpaque(true);
        questionLabel.setBackground(new Color(21, 101, 192)); // Azul médio

        // Painel de respostas com fundo mais claro para contraste
        JPanel answersPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        answersPanel.setBackground(new Color(240, 240, 240)); // Cinza claro
        answersPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Cores dos botões
        Color[] buttonColors = {
                new Color(229, 57, 53), // Vermelho
                new Color(67, 160, 71), // Verde
                new Color(255, 193, 7), // Amarelo
                new Color(30, 136, 229) // Azul
        };

        answerButtons = new JButton[4];
        for (int i = 0; i < 4; i++) {
            final int answerIndex = i;
            answerButtons[i] = new JButton("Opção " + (i + 1));
            answerButtons[i].setFont(new Font("Arial", Font.BOLD, 16));
            answerButtons[i].setBackground(buttonColors[i]);
            answerButtons[i].setForeground(Color.WHITE);
            answerButtons[i].setPreferredSize(new Dimension(200, 80));
            answerButtons[i].setOpaque(true);
            answerButtons[i].setContentAreaFilled(true);
            answerButtons[i].setBorderPainted(false);
            answerButtons[i].addActionListener(e -> selectAnswer(answerIndex));
            answerButtons[i].setEnabled(false);
            answersPanel.add(answerButtons[i]);
        }

        centerPanel.add(questionLabel, BorderLayout.NORTH);
        centerPanel.add(answersPanel, BorderLayout.CENTER);

        // ==== PLACAR ====
        scoreboardArea = new JTextArea(20, 15);
        scoreboardArea.setEditable(false);
        scoreboardArea.setBackground(new Color(48, 63, 159));
        scoreboardArea.setForeground(Color.WHITE);
        scoreboardArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scoreScrollPane = new JScrollPane(scoreboardArea);
        scoreScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE), "Placar",
                0, 0, new Font("Arial", Font.BOLD, 12), Color.WHITE));

        // Adiciona painéis à tela principal
        gamePanel.add(topPanel, BorderLayout.NORTH);
        gamePanel.add(centerPanel, BorderLayout.CENTER);
        gamePanel.add(scoreScrollPane, BorderLayout.EAST);
    }

    private void setupUDPListener() {
        new Thread(() -> {
            try {
                // Fechar socket anterior se existir
                if (udpSocket != null && !udpSocket.isClosed()) {
                    udpSocket.close();
                }

                udpSocket = new DatagramSocket(UDP_PORT);
                System.out.println("UDP escutando na porta: " + (UDP_PORT));

                while (true) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("UDP recebido: " + message);
                    processUDPMessage(message);
                }
            } catch (Exception e) {
                System.out.println("Erro no UDP: " + e.getMessage());
            }
        }).start();
    }

    private void connectToServer() {
        serverIP = serverIPField.getText().trim();
        playerName = playerNameField.getText().trim();

        if (serverIP.isEmpty() || playerName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha todos os campos!");
            return;
        }

        connectButton.setEnabled(false);
        statusLabel.setText("Conectando...");

        new Thread(() -> {
            try {
                tcpSocket = new Socket(serverIP, TCP_PORT);
                tcpIn = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
                tcpOut = new PrintWriter(tcpSocket.getOutputStream(), true);

                // Enviar nome do jogador
                tcpOut.println("JOIN|" + playerName);

                SwingUtilities.invokeLater(() -> {
                    connected = true;
                    showGamePanel();
                    statusLabel.setText("Conectado! Aguardando outros jogadores...");
                });

                // Thread para escutar mensagens TCP
                startTCPListener();

            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Erro ao conectar: " + e.getMessage());
                    connectButton.setEnabled(true);
                    statusLabel.setText("Erro na conexão");
                });
            }
        }).start();
    }

    private final Color[] buttonColors = {
            new Color(229, 57, 53), // Vermelho
            new Color(67, 160, 71), // Verde
            new Color(255, 193, 7), // Amarelo
            new Color(30, 136, 229) // Azul
    };

    private void startTCPListener() {
        new Thread(() -> {
            try {
                String inputLine;
                while ((inputLine = tcpIn.readLine()) != null) {
                    processTCPMessage(inputLine);
                }
            } catch (IOException e) {
                if (connected) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Conexão perdida com o servidor!");
                        showConnectionPanel();
                        connected = false;
                        connectButton.setEnabled(true);
                    });
                }
            }
        }).start();
    }

    private void processTCPMessage(String message) {
        String[] parts = message.split("\\|");

        SwingUtilities.invokeLater(() -> {
            switch (parts[0]) {
                case "JOINED":
                    questionLabel.setText("<html><div style='text-align: center; padding: 20px;'>Bem-vindo, " +
                            parts[1] + "!<br>Aguardando o jogo começar...</div></html>");
                    break;

                case "QUESTION":
                    currentQuestion = parts[1];
                    currentOptions = Arrays.copyOfRange(parts, 2, 6);
                    displayQuestion();
                    break;

                case "SCOREBOARD":
                    displayScoreboard(parts);
                    break;

                case "GAME_END":
                    JOptionPane.showMessageDialog(this, "Jogo finalizado! Obrigado por participar!");
                    canAnswer = false;
                    for (JButton button : answerButtons) {
                        button.setEnabled(false);
                    }
                    break;
            }
        });
    }

    private void processUDPMessage(String message) {
        String[] parts = message.split("\\|");

        SwingUtilities.invokeLater(() -> {
            switch (parts[0]) {
                case "TIME":
                    int timeLeft = Integer.parseInt(parts[1]);
                    //timerLabel.setText("Tempo: " + timeLeft);

                    if (timeLeft <= 0) {
                        canAnswer = false;
                        for (JButton button : answerButtons) {
                            button.setEnabled(false);
                        }
                    }
                    break;

                case "PLAYER_ANSWERED":
                    // Poderia mostrar notificação de que alguém respondeu
                    System.out.println("Jogador respondeu: " + parts[1]);
                    break;
            }
        });
    }

    private void displayQuestion() {
        questionLabel.setText("<html><div style='text-align: center; padding: 20px;'>" +
                currentQuestion + "</div></html>");

        for (int i = 0; i < 4; i++) {
            answerButtons[i].setText("<html><div style='text-align: center;'>" +
                    currentOptions[i] + "</div></html>");
            answerButtons[i].setBackground(buttonColors[i]); // <- Volta à cor original
            answerButtons[i].setEnabled(true);
        }

        canAnswer = true;
    }

    private void selectAnswer(int answerIndex) {
        if (!canAnswer)
            return;

        // Desabilitar todos os botões após responder
        canAnswer = false;
        for (JButton button : answerButtons) {
            button.setEnabled(false);
        }

        // Enviar resposta via TCP
        tcpOut.println("ANSWER|" + answerIndex);
    }

    private void displayScoreboard(String[] parts) {
        StringBuilder sb = new StringBuilder("=== PLACAR ===\n\n");

        for (int i = 1; i < parts.length; i++) {
            String[] playerData = parts[i].split(":");
            if (playerData.length == 2) {
                sb.append((i) + "º ").append(playerData[0])
                        .append(": ").append(playerData[1]).append(" pts\n");
            }
        }

        scoreboardArea.setText(sb.toString());
    }

    private void showConnectionPanel() {
        CardLayout cl = (CardLayout) getContentPane().getLayout();
        cl.show(getContentPane(), "CONNECTION");
        setTitle("Quiz Client - Conectar ao Servidor");
    }

    private void showGamePanel() {
        CardLayout cl = (CardLayout) getContentPane().getLayout();
        cl.show(getContentPane(), "GAME");
        setTitle("Quiz Client - " + playerName + " conectado");
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> new QuizClient());
    }
}