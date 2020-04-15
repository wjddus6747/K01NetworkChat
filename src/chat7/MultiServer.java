package chat7;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class MultiServer
{
	protected Connection con;
	protected Statement stmt;

	static ServerSocket serverSocket = null;
	static Socket socket = null;
	// 클라이언트 정보저장을 위한 Map컬렉션 정의
	Map<String, PrintWriter> clientMap;
	// 귓속말 하는 사람들이 저장된 HashMap
	HashMap<String, String> SercetMap = new HashMap<>();

	// 생성자
	public MultiServer()
	{
		// 클라이언트의 이름과 출력스트림을 저장할 HashMap생성
		clientMap = new HashMap<String, PrintWriter>();
		// HashMap동기화 설정. 쓰레드가 사용자정보에 동시에 접근하는것을 차단한다.
		Collections.synchronizedMap(clientMap);
	}

	// 서버 초기화
	public void init()
	{
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

			///////// 접속대기

			while (true)
			{
				socket = serverSocket.accept();
				/*
				 * 클라이언트의 메세지를 모든 클라이언트에게 전달하기 위한 쓰레드 생성 및 start...
				 */
				Thread mst = new MultiServerT(socket);
				mst.start();
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		} finally
		{
			try
			{
				if (stmt != null)
				{
					stmt.close();
				}
				if (con != null)
				{
					con.close();
				}
				System.out.println("서버 종료");
				serverSocket.close();

			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	// 메인메소드 : Server객체를 생성한후 초기화 한다.
	public static void main(String[] args)
	{
		MultiServer ms = new MultiServer();
		ms.init();
	}

	// 접속된 모든 클라이언트에게 메세지를 전달하는 역할의 메소드
	public void sendAllmsg(String name, String msg)
	{
		// Map에 저장된 객체의 키값(이름)을 먼저 얻어온다.
		Iterator<String> it = clientMap.keySet().iterator();

		// 저장된 객체(클라이언트)의 갯수만큼 반복한다.
		while (it.hasNext())
		{
			try
			{
				// 각 클라이언트의 PrintWriter객체를 얻어온다.
				// key값으로 value값을 찾는다.-> clientMap.get(it.next())
				PrintWriter it_out = (PrintWriter) clientMap.get(it.next());

				// 클라이언트에게 메세지를 전달한다.
				/*
				 * 매개변수 name이 있는 경우에는 이름+메세지 없는 경우에는 메세지만 클라이언트로 전달한다.
				 */
				if (name.equals(""))
				{
					// [ 서버 ] 클라이언트로 한글을 보낼 때 : UTF-8로 인코딩
					it_out.println(URLEncoder.encode(msg, "UTF-8"));
				} else
				{
					it_out.println(URLEncoder.encode("[" + name + "]:" + msg, "UTF-8"));
				}
			} catch (Exception e)
			{
				System.out.println("예외:" + e);
			}
		}
	}

	// /list를 입력했을때 name값 출력
	public void rslist(String name, Set<String> set)
	{
		PrintWriter it_out = (PrintWriter) clientMap.get(name);

		for (Iterator<String> iterator = set.iterator(); iterator.hasNext();)
		{
			String string = (String) iterator.next();
			it_out.println(string);
		}
	}

	// 귓속말
	public void secret(String name, String s)
	{
		StringTokenizer st = new StringTokenizer(s, " "); 
		String[] array = new String[st.countTokens()];
		for (int i = 0; i < array.length; i++)
		{
			array[i] = st.nextToken();
		}
		PrintWriter in_out = (PrintWriter) clientMap.get(array[1]);
		String message = array[2];
		in_out.println(array[1] + "님의 귓속말:" + message);
	}

	// 귓속말 고정/풀기 기능
	public void secretlock(String name, String s)
	{
		if (SercetMap.containsKey(name) == true)
		{
			SercetMap.remove(name);
		} else if (SercetMap.containsKey(name) == false)
		{
			SercetMap.put(name, s);
		}
		if (SercetMap.containsKey(name) == true)
		{
			secret(name, s);
		}
	}

	// 내부클래스
	class MultiServerT extends Thread
	{
		// 멤버변수
		Socket socket;
		PrintWriter out = null;
		BufferedReader in = null;

		// 생성자 : Socket을 기반으로 입출력 스트림을 생성한다.
		public MultiServerT(Socket socket)
		{
			this.socket = socket;
			try
			{
				// [ 서버 ] 클라이언트에서 올라온 한글 데이터 받을 때 : UTF-8 로 인코딩
				out = new PrintWriter(this.socket.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), "UTF-8"));
			} catch (Exception e)
			{
				System.out.println("예외:" + e);
			}
		}

		@Override
		public void run()
		{
			// 클라이언트로부터 전송된 "대화명"을 저장할 변수
			String name = "";
			// 메세지 저장용 변수
			String s = "";

			try
			{
				// 클라이언트의 이름을 읽어와서 저장
				name = in.readLine();
				// [ 서버 ] 클라이언트에서 올라온 한글 데이터 사용할 때 : UTF-8로 디코딩
				name = URLDecoder.decode(name, "UTF-8");

				// 접속한 클라이언트에게 새로운 사용자의 입장을 알림.
				// 접속자를 제외한 나머지 클라이언트만 입장메세지를 받는다.
				sendAllmsg("", name + "님이 입장하셨습니다.");

				// 현재 접속한 클라이언트를 HashMap에 저장한다.
				clientMap.put(name, out);

				// HashMap에 저장된 객체의 수로 접속자수를 파악할 수 있다.
				System.out.println(name + "접속");
				System.out.println("현재 접속자 수는 " + clientMap.size() + "명 입니다.");

				// 입력한 메세지는 모든 클라이언트에게 Echo된다.
				// 클라이언트로부터 받은 메세지를 읽어 명령어를 분석
				while (in != null)
				{
					if (s != null)
					{
						s = in.readLine();
						s = URLDecoder.decode(s, "UTF-8");
						System.out.println(name + ">>" + s);
						if (s.equals("/list"))
						{
							System.out.println(clientMap.keySet());
							rslist(name, clientMap.keySet());
						}
						if (s.indexOf("/to") == 0)
						{
							StringTokenizer st = new StringTokenizer(s);
							String[] arr = new String[st.countTokens()];
							for (int i = 0; i < arr.length; i++)
							{
								arr[i] = st.nextToken();
							}
							if (arr.length == 2)
							{
								secret(name, s);
							} else if (arr.length != 2)
							{
								secretlock(name, s);
							}

						} else
						{
							sendAllmsg(name, s);
						}
					}
					if (s == null)
						break;

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
			} catch (Exception e)
			{
				System.out.println("예외:" + e);
				e.printStackTrace();
			} finally
			{ /*
				 * 클라이언트가 접속을 종료하면 예외가 발생하게 되어 finally로 넘어오게 된다. 이때 "대화명"을 통해 remove()시켜준다.
				 */
				clientMap.remove(name);
				sendAllmsg(" ", name + "님이 퇴장하셨습니다,");
				// 퇴장하는 클라이언트의 쓰레드명을 보여준다.
				System.out.println(name + "[" + Thread.currentThread().getName() + "]퇴장");
				System.out.println("현재접속자 수는 " + clientMap.size() + "명입니다.");
				try
				{
					in.close();
					out.close();
					socket.close();
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}
}
