# fun gamers bot
Here lies team "fun gamers" 2012 [Battlecode](http://battlecode.org) bot, codenamed "InsaneAI."  
Top ranked on the scrimmage server, at 88-8 (1841), and first in the final tournament.  

**Team Members**: Haitao Mao, Cory Li, Yanping Chen, Justin Venezuela.

## Running
The `.project` was created in Eclise 3.7 and should load fine if using the same IDE. The Battlecode engine can either be built with `ant run` or via the `BattlecodeClient.launch` launch configuration.

## Notes
* The `ducks` package is the main branch of development. All other packages are frozen bots either cut from the main branch or written as tests of some sort.
* There are a few external python scripts in the main folder used to scrape the main battlecode scrimmage rankings site and determine which teams lost against which other teams.
* The `report` folder contains the source and images to our final report, (ironically) winner of the best strategy report award.
* There are a number of custom maps contained in the `maps` folder designed to test our bot against certain edge cases or interesting situations

## Points of Interest
There are a number of interesting features in the architecture of the bot. A few of them, in no particular order:

* Full implementation of tangentbug that calculates paths with spare bytecodes
* Highly bytecode optimized hibernation system
* Message attack / team detection subsystem, in particular keyed to detect Team16, the second place 2012 finalist
* Full battlefield awareness for attackers via Extended Radar broadcasts

## Issues
There are a number of known & unresolved issues with the bot:

* The team detection flag is not propagated on rounds after it is set.  The proper fix would be for Archon 0 to check if the flag was set the previous round and then set it again for the next. This almost cost us the final tournament.
* Archons randomly stop moving for some reason -- see the 15th match of the 2012 final tournament for reference.

## Versions
A number of builds were created throughout the competition, each named after a Starcraft 2 AI difficulty setting. The corresponding builds can be found on the downloads page. InsaneAI was the last built version that ultimately competed in the final tournament.  For those diving through the codebase looking to see exactly which version was used for which tournament, the submission logs are recorded below:

	InsaneAIv4 from 12-01-30 20:58:29: success
		null
	InsaneAIv3 from 12-01-30 20:49:42: success
		null
	InsaneAIv2 from 12-01-30 20:33:33: success
		null
	BrutalAI from 12-01-28 09:03:05: success
		null
	veryharderai from 12-01-23 20:55:07: success
		null
	veryharderai from 12-01-23 20:53:02: failure
		BUILD FAILED
		~/build.xml:438: Java returned: 42
		Verifier failed to find your class, make sure you submitted the jar file with the .java files.
		Make sure your code package is team047, and in the team047 folder.
		Total time: 6 seconds
	VeryHardAI from 12-01-23 07:32:54: success
		null
	hardai from 12-01-22 07:06:27: success
		null
	HTpro from 12-01-19 18:33:40: success
		null
	HTnoob from 12-01-19 05:03:11: success
		null

