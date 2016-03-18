package entry;

import basic.Config;
import controller.ClientCore;

public class ClientEntry {
	public static void main(String[] args) {
		Config.setValue("useragent",
				"Mozilla/5.0 (Windows NT 5.1; rv:2.0.1) Gecko/20100101 Firefox/4.0.1");
		if (args.length > 3)
			Config.setValue("useragent", args[3]);
		new ClientCore(Integer.valueOf(args[0]), Integer.valueOf(args[1]),
				Integer.valueOf(args[2]));
	}
}
