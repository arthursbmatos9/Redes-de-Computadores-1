import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class QuizServer extends JFrame {
    private static final int TCP_PORT = 5000;
    private static final int UDP_PORT = 6000;

    private ServerSocket tcpServer;
    private DatagramSocket udpSocket;
    private Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private List<Question> questions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private boolean gameActive = false;
    private javax.swing.Timer questionTimer;
    private int timeRemaining = 2;

    // GUI Components
    private JTextArea logArea;
    private JButton startGameButton;
    private JLabel statusLabel;
    private JLabel playersLabel;
    private JProgressBar timerProgressBar;

    public QuizServer() {
        initializeQuestions();
        setupGUI();
        startServer();
    }

    private void initializeQuestions() {
        questions.add(new Question("Qual é a capital do Brasil?",
                new String[]{"São Paulo", "Rio de Janeiro", "Brasília", "Belo Horizonte"}, 2));
        questions.add(new Question("Quantos planetas há no sistema solar?",
                new String[]{"7", "8", "9", "10"}, 1));
        questions.add(new Question("Qual é o maior oceano do mundo?",
                new String[]{"Atlântico", "Pacífico", "Índico", "Ártico"}, 1));
        questions.add(new Question("Em que ano o Brasil foi descoberto?",
                new String[]{"1500", "1501", "1499", "1502"}, 0));
        questions.add(new Question("Qual é a linguagem de programação mais usada para desenvolvimento web?",
                new String[]{"Python", "Java", "JavaScript", "C++"}, 2));
        questions.add(new Question("Quem escreveu 'Dom Casmurro'?",
                new String[]{"Machado de Assis", "José de Alencar", "Clarice Lispector", "Jorge Amado"}, 0));
        questions.add(new Question("Qual é o elemento químico com símbolo 'O'?",
                new String[]{"Ouro", "Oxigênio", "Ósmio", "Oganessônio"}, 1));  
        questions.add(new Question("Qual é o maior continente do mundo?",
                new String[]{"África", "América", "Ásia", "Europa"}, 2));
        questions.add(new Question("Qual é o idioma mais falado no mundo?",
                new String[]{"Inglês", "Mandarim", "Espanhol", "Hindi"}, 1));
    }

    private void setupGUI() {
        setTitle("Quiz Server - TCP: " + TCP_PORT + " | UDP: " + UDP_PORT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(Color.WHITE);

        // Painel superior
        JPanel topPanel = new JPanel(new GridLayout(2, 3, 10, 5));
        topPanel.setBorder(BorderFactory.createTitledBorder("Controle do Servidor"));

        statusLabel = new JLabel("Status: Aguardando jogadores...");
        statusLabel.setForeground(Color.BLUE);
        playersLabel = new JLabel("Jogadores conectados: 0");

        startGameButton = new JButton("Iniciar Jogo");
        startGameButton.setEnabled(false);
        startGameButton.setBackground(new Color(34, 139, 34));
        startGameButton.setForeground(Color.WHITE);
        startGameButton.setFocusPainted(false);
        startGameButton.addActionListener(e -> startGame());

        timerProgressBar = new JProgressBar(0, 2);
        timerProgressBar.setValue(2);
        timerProgressBar.setStringPainted(true);
        timerProgressBar.setForeground(Color.RED);

        topPanel.add(statusLabel);
        topPanel.add(playersLabel);
        topPanel.add(startGameButton);
        topPanel.add(new JLabel("Tempo restante:"));
        topPanel.add(timerProgressBar);
        topPanel.add(new JLabel(""));

        // Área de log
        logArea = new JTextArea(20, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Log do Servidor"));

        // Botão limpar log
        JButton clearLogButton = new JButton("Limpar Log");
        clearLogButton.addActionListener(e -> logArea.setText(""));
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.add(scrollPane, BorderLayout.CENTER);
        logPanel.add(clearLogButton, BorderLayout.SOUTH);

        // Adiciona componentes à janela
        add(topPanel, BorderLayout.NORTH);
        add(logPanel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        log("Servidor iniciado!");
        log("IP do servidor: " + getLocalIPAddress());
        log("Porta TCP: " + TCP_PORT);
        log("Porta UDP: " + UDP_PORT);
    }

    private String getLocalIPAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                 en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                     enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            log("Erro ao obter IP: " + ex.getMessage());
        }
        return "localhost";
    }

    private void startServer() {
        new Thread(() -> {
            try {
                tcpServer = new ServerSocket(TCP_PORT);
                log("Servidor TCP iniciado na porta " + TCP_PORT);
                while (true) {
                    Socket clientSocket = tcpServer.accept();
                    String clientId = clientSocket.getRemoteSocketAddress().toString();
                    log("Cliente conectado: " + clientId);

                    ClientHandler handler = new ClientHandler(clientSocket, clientId);
                    clients.put(clientId, handler);
                    new Thread(handler).start();
                    updatePlayersCount();
                }
            } catch (IOException e) {
                log("Erro no servidor TCP: " + e.getMessage());
            }
        }).start();

        new Thread(() -> {
            try {
                udpSocket = new DatagramSocket(UDP_PORT);
                log("Servidor UDP iniciado na porta " + UDP_PORT);
                while (true) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    log("UDP recebido: " + message + " de " + packet.getAddress());
                }
            } catch (IOException e) {
                log("Erro no servidor UDP: " + e.getMessage());
            }
        }).start();
    }

    private void updatePlayersCount() {
        SwingUtilities.invokeLater(() -> {
            playersLabel.setText("Jogadores conectados: " + clients.size());
            startGameButton.setEnabled(clients.size() >= 1);
        });
    }

    private void startGame() {
        if (clients.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum jogador conectado!");
            return;
        }

        gameActive = true;
        currentQuestionIndex = 0;
        startGameButton.setEnabled(false);
        statusLabel.setText("Status: Jogo em andamento");
        statusLabel.setForeground(new Color(0, 128, 0));
        log("Jogo iniciado com " + clients.size() + " jogadores!");
        sendNextQuestion();
    }

    private void sendNextQuestion() {
        if (currentQuestionIndex >= questions.size()) {
            endGame();
            return;
        }

        Question question = questions.get(currentQuestionIndex);
        String questionData = "QUESTION|" + question.getQuestion() + "|" +
                String.join("|", question.getOptions());

        for (ClientHandler client : clients.values()) {
            client.sendMessage(questionData);
        }

        log("Pergunta " + (currentQuestionIndex + 1) + " enviada: " + question.getQuestion());
        startQuestionTimer();
    }

    private void startQuestionTimer() {
        timeRemaining = 2;
        timerProgressBar.setValue(2);

        if (questionTimer != null) {
            questionTimer.stop();
        }

        questionTimer = new javax.swing.Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timeRemaining--;
                timerProgressBar.setValue(timeRemaining);
                sendTimeUpdate();
                if (timeRemaining <= 0) {
                    questionTimer.stop();
                    processQuestionEnd();
                }
            }
        });
        questionTimer.start();
    }

    private void sendTimeUpdate() {
        String timeMessage = "TIME|" + timeRemaining;

        for (ClientHandler client : clients.values()) {
            try {
                // Send via UDP
                InetAddress clientAddress = client.getClientSocket().getInetAddress();
                DatagramPacket packet = new DatagramPacket(
                        timeMessage.getBytes(),
                        timeMessage.length(),
                        clientAddress,
                        UDP_PORT 
                );
                System.out.println("Enviando tempo via UDP: " + timeMessage);
                udpSocket.send(packet);

                // Send via TCP
                client.sendMessage(timeMessage);
            } catch (IOException e) {
                log("Erro enviando tempo: " + e.getMessage());
            }
        }
    }

    private void processQuestionEnd() {
        Question currentQuestion = questions.get(currentQuestionIndex);

        for (ClientHandler client : clients.values()) {
            if (client.hasAnswered() && client.getLastAnswer() == currentQuestion.getCorrectAnswer()) {
                int points = Math.max(100 - (30 - timeRemaining) * 2, 10);
                client.addPoints(points);
                System.out.println("Cliente " + client.getClientId() + " acertou! +" + points + " pontos");
            }
            client.resetAnswer();
        }

        sendScoreboard();

        javax.swing.Timer nextQuestionTimer = new javax.swing.Timer(200, e -> {
            currentQuestionIndex++;
            sendNextQuestion();
        });
        nextQuestionTimer.setRepeats(false);
        nextQuestionTimer.start();
    }

    private void sendScoreboard() {
        StringBuilder scoreboard = new StringBuilder("SCOREBOARD");
        List<ClientHandler> sortedClients = new ArrayList<>(clients.values());
        sortedClients.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));

        for (ClientHandler client : sortedClients) {
            scoreboard.append("|").append(client.getPlayerName()).append(":").append(client.getScore());
        }

        String scoreMessage = scoreboard.toString();
        for (ClientHandler client : clients.values()) {
            client.sendMessage(scoreMessage);
        }

        log("Placar enviado: " + scoreMessage);
    }

    private void endGame() {
        gameActive = false;
        statusLabel.setText("Status: Jogo finalizado");
        statusLabel.setForeground(Color.RED);
        startGameButton.setEnabled(true);

        for (ClientHandler client : clients.values()) {
            client.sendMessage("GAME_END");
        }

        sendScoreboard();
        log("Jogo finalizado!");
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + new Date() + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientId;
        private String playerName;
        private int score = 0;
        private boolean answered = false;
        private int lastAnswer = -1;

        public ClientHandler(Socket socket, String id) {
            this.clientSocket = socket;
            this.clientId = id;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                log("Erro criando handler: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    processMessage(inputLine);
                }
            } catch (IOException e) {
                log("Cliente desconectado: " + clientId);
            } finally {
                cleanup();
            }
        }

        private void processMessage(String message) {
            String[] parts = message.split("\\|");

            switch (parts[0]) {
                case "JOIN":
                    playerName = parts[1];
                    sendMessage("JOINED|" + playerName);
                    log("Jogador entrou: " + playerName);
                    break;
                case "ANSWER":
                    if (gameActive && !answered) {
                        lastAnswer = Integer.parseInt(parts[1]);
                        answered = true;
                        log("Resposta recebida de " + playerName + ": " + lastAnswer);
                        sendAnswerNotification();
                    }
                    break;
            }
        }

        private void sendAnswerNotification() {
            String notification = "PLAYER_ANSWERED|" + playerName;
            for (ClientHandler client : clients.values()) {
                try {
                    InetAddress clientAddress = client.getClientSocket().getInetAddress();
                    DatagramPacket packet = new DatagramPacket(
                            notification.getBytes(),
                            notification.length(),
                            clientAddress,
                            UDP_PORT + 1
                    );
                    udpSocket.send(packet);
                } catch (IOException e) {
                    log("Erro enviando notificação UDP: " + e.getMessage());
                }
            }
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        private void cleanup() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
                clients.remove(clientId);
                updatePlayersCount();
            } catch (IOException e) {
                log("Erro fechando conexão: " + e.getMessage());
            }
        }

        public Socket getClientSocket() { return clientSocket; }
        public String getClientId() { return clientId; }
        public String getPlayerName() { return playerName != null ? playerName : "Jogador"; }
        public int getScore() { return score; }
        public void addPoints(int points) { this.score += points; }
        public boolean hasAnswered() { return answered; }
        public int getLastAnswer() { return lastAnswer; }
        public void resetAnswer() { answered = false; lastAnswer = -1; }
    }

    private static class Question {
        private String question;
        private String[] options;
        private int correctAnswer;

        public Question(String question, String[] options, int correctAnswer) {
            this.question = question;
            this.options = options;
            this.correctAnswer = correctAnswer;
        }

        public String getQuestion() { return question; }
        public String[] getOptions() { return options; }
        public int getCorrectAnswer() { return correctAnswer; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QuizServer());
    }
}
