public class Calculadora {
    public String sayHello(String nome, String sobrenome) {
        return "Fala " + nome + " " + sobrenome;
    }

    public double soma(double oper1, double oper2) {
        return oper1 + oper2;
    }

    //Operação de subtração adicionada
    public double subtrai(double oper1, double oper2) {
        return oper1 - oper2;
    }

    //Operação de multiplicação adicionada
    public double multiplica(double oper1, double oper2) {
        return oper1 * oper2;
    }

    //Operação de divisão adicionada
    public double divide(double oper1, double oper2) {
        if (oper2 == 0) throw new ArithmeticException("divisao por zero");
        return oper1 / oper2;
    }

    //Escolhe a operacao pelo simbolo. Usado quando o servidor avalia uma expressao.
    public double executa(char operador, double oper1, double oper2) {
        switch (operador) {
            case '-':
                return subtrai(oper1, oper2);
            case '*':
                return multiplica(oper1, oper2);
            case '/':
                return divide(oper1, oper2);
            default:
                return soma(oper1, oper2);
        }
    }
}