import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Calculadora implements ICalculadora {

    private static final long serialVersionUID = 1L;

    private static int chamadas = 0;

    public double soma(double a, double b) throws RemoteException {
        System.out.println("Metodo soma chamado " + chamadas++);
        return a + b;
    }

    public double subtrai(double a, double b) throws RemoteException {
        System.out.println("Metodo subtrai chamado " + chamadas++);
        return a - b;
    }

    public double divide(double a, double b) throws RemoteException {
        System.out.println("Metodo divide chamado " + chamadas++);
        if (b == 0) throw new ArithmeticException("divisao por zero");
        return a / b;
    }

    public double multiplica(double a, double b) throws RemoteException {
        System.out.println("Metodo multiplica chamado " + chamadas++);
        return a * b;
    }

    //Avalia a expressao inteira aqui no servidor
    public double avaliarExpressao(String expr) throws RemoteException {
        System.out.println("Metodo avaliarExpressao chamado " + chamadas++);
        return Expressao.avaliar(expr, (op, x, y) -> {
            switch (op) {
                case '-':
                    return x - y;
                case '*':
                    return x * y;
                case '/':
                    if (y == 0) throw new ArithmeticException("divisao por zero");
                    return x / y;
                default:
                    return x + y;
            }
        });
    }

    public static void main(String[] args) throws AccessException, RemoteException, AlreadyBoundException {
        Calculadora calculadora = new Calculadora();
        Registry reg = null;
        ICalculadora stub = (ICalculadora) UnicastRemoteObject.
                exportObject(calculadora, 1100);
        try {
            System.out.println("Creating registry...");
            reg = LocateRegistry.createRegistry(1099);
        } catch (Exception e) {
            try {
                reg = LocateRegistry.getRegistry(1099);
            } catch (Exception e1) {
                System.exit(0);
            }
        }
        reg.rebind("calculadora", stub);
    }
}