package chat2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class MultiServer
{

	protected static Connection con;
	protected static Statement stmt;
	protected ResultSet rs;
	
	public static void main(String[] args)
	{
		ServerSocket serverSocket = null;
		Socket socket = null;
		PrintWriter out = null;
		BufferedReader in = null;
		String s = ""; //클라이언트의 메세지를 저장 
		String name = ""; //클라이언트의 이름을 저장 

		try
		{
			// DB연결
			try
			{
			Class.forName("oracle.jdbc.OracleDriver");
			con = DriverManager.getConnection("jdbc:oracle:thin://@localhost:1521:orcl", "kosmo", "1234");
			System.out.println("오라클 DB연결성공");
			} catch (Exception e)
			{
			System.out.println("DB연결실패");
			e.printStackTrace();
			}
			serverSocket = new ServerSocket(9999);
			System.out.println("서버가 시작되었습니다.");

			socket = serverSocket.accept();
			
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			/*
			 클라이언트가 서버로 전송하는 최초의 메세지는 "대화명"이므로
			 메세지를 읽은후 변수에 저장하고 클라이언트쪽으로 Echo해준다.
			 */
			if(in !=null) {
				name = in.readLine();
				System.out.println(name + "접속");
				out.println(">" + name + "님이 접속했습니다.");
			}
			
			/*
			 두번째 메세지부터는 실제 대화내용이므로 읽어와서 로그로 출력하고
			 동시에 클라이언트로 Echo한다.
			 */
			while(in!=null) {
				s=in.readLine();
				if(s==null) {
					break;
				}
				System.out.println(name+"==>"+ s);
				out.println(">" + name + "==>"+s);
				// JDBC
				try
				{
					stmt = con.createStatement();
					String query = "INSERT INTO chating_tb VALUES(seq_chat.nextval, '" + name + "', '" + s
							+ "', sysdate)";

					stmt.executeUpdate(query);
				} catch (Exception e)
				{
					System.out.println("쿼리 실행문제");
					e.printStackTrace();
				}
			}
			System.out.println("Bye...!!");
		} catch (Exception e)
		{
			System.out.println("예외1:"+e);
		} finally
		{
			try
			{
				in.close();
				out.close();
				socket.close();
				serverSocket.close();
			} catch (Exception e)
			{
				System.out.println("에외2:"+e);
				//e.printStackTrace();
			}
		}
	}
}
