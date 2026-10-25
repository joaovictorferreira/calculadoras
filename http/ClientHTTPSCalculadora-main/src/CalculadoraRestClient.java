import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class CalculadoraRestClient extends JFrame {
    private JTextField operador1TextField;
    private JTextField operador2TextField;
    private JComboBox<String> operacaoComboBox;
    private JTextField expressaoTextField;
    private JComboBox<String> modoComboBox;
    private JTextField resultadoTextField;

    //Endereco da pagina PHP servidora
    public static final String URL_SERVIDOR = "http://localhost:8000/calculadora.php";

    public CalculadoraRestClient() {
        super("Calculadora REST Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        add(panel);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.add(inputPanel, BorderLayout.NORTH);

        inputPanel.add(new JLabel("Operador 1:"));
        operador1TextField = new JTextField(10);
        inputPanel.add(operador1TextField);

        inputPanel.add(new JLabel("Operador 2:"));
        operador2TextField = new JTextField(10);
        inputPanel.add(operador2TextField);

        inputPanel.add(new JLabel("Operacao:"));
        operacaoComboBox = new JComboBox<>(new String[]{"soma", "subtracao", "multiplicacao", "divisao"});
        inputPanel.add(operacaoComboBox);

        JButton calcularButton = new JButton("Calcular");
        calcularButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calcular();
            }
        });
        inputPanel.add(calcularButton);

        //Linha de expressao matematica
        JPanel exprPanel = new JPanel();
        exprPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.add(exprPanel, BorderLayout.CENTER);

        exprPanel.add(new JLabel("Expressao:"));
        expressaoTextField = new JTextField(18);
        exprPanel.add(expressaoTextField);

        modoComboBox = new JComboBox<>(new String[]{"decompor no cliente", "calcular no servidor"});
        exprPanel.add(modoComboBox);

        JButton exprButton = new JButton("Calcular expressao");
        exprButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calcularExpressao();
            }
        });
        exprPanel.add(exprButton);

        JPanel outputPanel = new JPanel();
        outputPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.add(outputPanel, BorderLayout.SOUTH);

        outputPanel.add(new JLabel("Resultado da Operacao:"));
        resultadoTextField = new JTextField(20);
        resultadoTextField.setEditable(false);
        outputPanel.add(resultadoTextField);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    //Operacao simples: envia os dois operandos e a operacao escolhida no combo
    private void calcular() {
        String oper1 = operador1TextField.getText();
        String oper2 = operador2TextField.getText();
        int operacao = operacaoComboBox.getSelectedIndex() + 1;

        //roda fora da thread grafica para a janela nao congelar durante as tentativas de retry
        new Thread(() -> {
            String texto;
            try {
                texto = "" + chamarOperacao(operacao, oper1, oper2);
            } catch (Exception e) {
                texto = "ERRO: " + e.getMessage();
            }
            mostrar(texto);
        }).start();
    }

    //Expressao matematica, nas duas abordagens exigidas pela atividade
    private void calcularExpressao() {
        String expr = expressaoTextField.getText();
        boolean decompor = modoComboBox.getSelectedIndex() == 0;

        new Thread(() -> {
            String texto;
            try {
                texto = "" + (decompor ? calcularDecompondo(expr) : chamarExpressao(expr));
            } catch (Exception e) {
                texto = "ERRO: " + e.getMessage();
            }
            mostrar(texto);
        }).start();
    }

    private void mostrar(String texto) {
        EventQueue.invokeLater(() -> resultadoTextField.setText(texto));
    }

    //Abordagem 1: o cliente interpreta a expressao e faz uma requisicao HTTP por operacao
    public static double calcularDecompondo(String expr) {
        return Expressao.avaliar(expr, (op, a, b) -> {
            try {
                int operacao;
                switch (op) {
                    case '-':
                        operacao = 2;
                        break;
                    case '*':
                        operacao = 3;
                        break;
                    case '/':
                        operacao = 4;
                        break;
                    default:
                        operacao = 1;
                }
                double r = chamarOperacao(operacao, "" + a, "" + b);
                System.out.println("   requisicao: " + a + " " + op + " " + b + " -> " + r);
                return r;
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        });
    }

    //Abordagem 2: envia a expressao inteira (operacao 5) para o PHP calcular
    public static double chamarExpressao(String expr) throws IOException {
        return lerResultado(postComRetry("operacao=5&expr=" + URLEncoder.encode(expr, "UTF-8")));
    }

    public static double chamarOperacao(int operacao, String oper1, String oper2) throws IOException {
        String corpo = "operacao=" + operacao + "&oper1=" + oper1 + "&oper2=" + oper2;
        return lerResultado(postComRetry(corpo));
    }

    //Le o JSON devolvido pelo PHP: ou o campo resultado, ou o campo erro
    private static double lerResultado(String json) throws IOException {
        String erro = campoJson(json, "erro");
        if (erro != null) throw new RuntimeException(erro);

        String valor = campoJson(json, "resultado");
        if (valor == null) throw new IOException("resposta fora do formato esperado: " + json);

        return Double.parseDouble(valor);
    }

    private static String campoJson(String json, String campo) {
        String marca = "\"" + campo + "\":";
        int p = json.indexOf(marca);
        if (p < 0) return null;

        String resto = json.substring(p + marca.length()).trim();
        if (resto.startsWith("\"")) return resto.substring(1, resto.indexOf('"', 1));

        int fim = resto.indexOf(',');
        if (fim < 0) fim = resto.indexOf('}');
        return resto.substring(0, fim).trim();
    }

    //POLITICA DE RETRY
    //Repete a requisicao apenas em falhas temporarias (servidor fora do ar, timeout, erro 5xx),
    //esperando um tempo que dobra a cada tentativa. Erro de calculo nao e repetido.
    public static String postComRetry(String corpo) throws IOException {
        int maxTentativas = 3;
        long espera = 1000;

        for (int tentativa = 1; ; tentativa++) {
            try {
                return post(corpo);
            } catch (IOException e) {
                if (tentativa >= maxTentativas) {
                    System.out.println("[RETRY] desistindo apos " + maxTentativas + " tentativas: " + e.getMessage());
                    throw new IOException("servidor indisponivel apos " + maxTentativas + " tentativas");
                }
                System.out.println("[RETRY] tentativa " + tentativa + " falhou (" + e.getMessage()
                        + "), repetindo em " + espera + "ms");
                try {
                    Thread.sleep(espera);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                espera = espera * 2;
            }
        }
    }

    private static String post(String corpo) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(URL_SERVIDOR).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(corpo.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        //5xx indica problema temporario do servidor, entao vale repetir
        if (status >= 500) throw new IOException("servidor respondeu HTTP " + status);

        InputStream in = (status >= 400) ? conn.getErrorStream() : conn.getInputStream();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining());
        }
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new CalculadoraRestClient();
            }
        });
    }
}