package core;

public class ThreadManager {

	public static final int NORMAL = 0;
//	private static int threadCount;
	public static final int DAEMON = 1;

	/*
	 * Intend to do something with type param, but for now, useless
	 * 5/12/11 - No intention to use type param, too lazy to remove it
	 */
	public static void registerWork(int type, final Runnable runnable, String msg, String name) {
		if (msg != null)
			Logger.log(Logger.INFO, "ThreadManager", msg);
//			System.out.println("[" + ThreadManager.class.getCanonicalName() + "] " + s);
		Runnable rnnble = new Runnable() {

			@Override
			public void run() {
//				threadCount++;
//				System.out.println("threadCount++: " + threadCount);
				try {
					runnable.run();
				} catch (Exception e) {
					// one hopes this doesn't happen
					e.printStackTrace();
				}
//				threadCount--;
//				System.out.println("threadCount--: " + threadCount);
			}
		};
		Thread thred;
		if (name != null)
			thred = new Thread(rnnble, name);
		else
			thred = new Thread(rnnble);
		thred.setDaemon(type == DAEMON);
		thred.start();
	}

}
