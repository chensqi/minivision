package socket;

import basic.tools.Speeder;

public class StrReceiver extends BaseConnection {

	private static Speeder speeder = new Speeder(6000);

	@Override
	protected void transaction() throws Exception {
		int cnt = 0;
		for (;;) {
			String s = readString();
			speeder.trigger();
			String ip = socket.getInetAddress().toString().replace("/", "");
			if ((cnt++) % 100 == 0) {
				System.out.println(ip + " : " + s.length() + " "
						+ speeder.getSpeed());
			}
//			sendInt(1);
			if (s.length() == 0)
				break;
		}
	}
}
