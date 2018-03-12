package core;

import java.util.ArrayList;
import util.RequestHTTP;

public class Update {

	public static String loadUpdate(String remote_bl_URL) {
		String outversion = null;

		ArrayList<String> input = RequestHTTP.load(remote_bl_URL);
		String b[];

		for (int i = 0; i < input.size(); i++) {
			String l = input.get(i);

			if(l.startsWith("<version!")) {
				b=l.split("!");
				if (b.length == 5)
				{
					outversion = b[1] +"."+ b[2] +"."+ b[3];    	
				}
			}

			if(l.startsWith("<CLOSE_STREAM>"))//This is bad practice --> add to loop condition instead?
				break;
		}

		if(outversion != null) {
			Logger.log(Logger.INFO, "loadUpdate","Compare: " + Config.BUILD +" with "+ outversion);

			if(isOutdated(Config.BUILD, outversion)) {
				return language.langtext[52];
			} else {
				if(Config.allcheckupdate == 1) {
					Config.allcheckupdate = 0;
					return language.langtext[53];
				} else 
					return"close";
			}
		} else
			return language.langtext[54];
	}

	private static boolean isOutdated(String buildVersion, String outVersion) {
		boolean isOutdated = false;
		String[] buildVersionComponents = buildVersion.split("[.]");
		String[] outVersionComponents = outVersion.split("[.]");

		int buildVersionIndex = 0;
		int outVersionIndex = 0;


		while (buildVersionIndex < buildVersionComponents.length || outVersionIndex < outVersionComponents.length) {

			//If Version Index is within buildVersionComponents' size, set the current buildVersionComponent. Else, set it equal to 0
			final int currBuildVersionComponent = (buildVersionIndex < buildVersionComponents.length) ? 
					Integer.parseInt(buildVersionComponents[buildVersionIndex]) : 0;

					//If out Version Index is within out version components array, set it equal to the component at the index. Else, --> 0
					final int outBuildVersionComponent = (outVersionIndex < outVersionComponents.length) ?
							Integer.parseInt(outVersionComponents[outVersionIndex]) : 0;

							if (currBuildVersionComponent < outBuildVersionComponent) {
								isOutdated = true;
								break; //Let's figure out a way to avoid this break
							} else if (currBuildVersionComponent > outBuildVersionComponent) {
								// result is false
								break; //Let's figure out a way to avoid this break
							}

							buildVersionIndex++;
							outVersionIndex++;
		}

		return isOutdated;
	}
}

