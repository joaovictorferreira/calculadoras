import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ICalculadora extends Remote {

    public double soma(double a, double b) throws RemoteException;

    public double subtrai(double a, double b) throws RemoteException;

    public double divide(double a, double b) throws RemoteException;

    public double multiplica(double a, double b) throws RemoteException;

    //Recebe a expressao inteira e devolve o resultado calculado no servidor
    public double avaliarExpressao(String expr) throws RemoteException;
}
