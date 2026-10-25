/**
 * Interpreta expressoes como (10+15)*4.
 * Quem decide como cada operacao e executada e o Operacao recebido:
 * no servidor ele calcula local, no cliente ele faz uma chamada remota.
 */
public class Expressao {

    public interface Operacao {
        double aplicar(char op, double a, double b);
    }

    private final String s;
    private final Operacao op;
    private int i;

    //Ponto de entrada: avalia a expressao inteira e valida se sobrou lixo no final
    public static double avaliar(String texto, Operacao op) {
        Expressao e = new Expressao(texto, op);
        double v = e.soma();
        if (e.i < e.s.length()) throw new IllegalArgumentException("expressao invalida");
        return v;
    }

    private Expressao(String texto, Operacao op) {
        this.s = texto.replace(" ", "");
        this.op = op;
    }

    //Trata + e -
    private double soma() {
        double v = produto();
        while (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-'))
            v = op.aplicar(s.charAt(i++), v, produto());
        return v;
    }

    //Trata * e / . Fica num nivel mais interno que a soma, por isso resolve primeiro
    private double produto() {
        double v = valor();
        while (i < s.length() && (s.charAt(i) == '*' || s.charAt(i) == '/'))
            v = op.aplicar(s.charAt(i++), v, valor());
        return v;
    }

    //Trata um numero ou um grupo entre parenteses
    private double valor() {
        if (s.charAt(i) == '(') {
            i++;
            double v = soma();
            if (i >= s.length() || s.charAt(i) != ')') throw new IllegalArgumentException("falta fechar parentese");
            i++;
            return v;
        }
        int ini = i;
        while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) i++;
        return Double.parseDouble(s.substring(ini, i));
    }
}