#!/usr/bin/env sclang

/*
 * Copyright (c) 2013 Hanspeter Portner (dev@open-music-kontrollers.ch)
 * 
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any damages
 * arising from the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 *     1. The origin of this software must not be misrepresented; you must not
 *     claim that you wrote the original software. If you use this software
 *     in a product, an acknowledgment in the product documentation would be
 *     appreciated but is not required.
 * 
 *     2. Altered source versions must be plainly marked as such, and must not be
 *     misrepresented as being the original software.
 * 
 *     3. This notice may not be removed or altered from any source
 *     distribution.
 */

//Server.supernova;
s.options.blockSize = 0x10;
s.options.memSize = 0x10000;
s.latency = nil;
s.boot;

s.doWhenBooted({
	var hostname, tx, chimconf, rate, sidOffset, gidOffset;
	
	hostname = "hostname".unixCmdGetStdOutLines[0]++".local";

	gidOffset = 100;
	sidOffset = 200;

	thisProcess.openUDPPort(4444); // open port 4444 for listening to chimaera configuration replies

	tx = NetAddr ("chimaera.local", 4444);

	chimconf = ChimaeraConf(s, tx, tx);

	rate = 3000;
	chimconf.sendMsg("/engines/reset");
	chimconf.sendMsg("/engines/address", hostname++":"++s.addr.port); // send output stream to port 3333
	chimconf.sendMsg("/engines/offset", 0.002);

	chimconf.sendMsg("/sensors/rate", rate);
	chimconf.sendMsg("/sensors/group/reset"); // reset groups
	chimconf.sendMsg("/sensors/group/attributes/0", 0.0, 1.0, false, true, false); // add group
	chimconf.sendMsg("/sensors/group/attributes/1", 0.0, 1.0, true, false, false); // add group

	chimconf.sendMsg("/engines/scsynth/enabled", true); // enable scsynth output engine
	chimconf.sendMsg("/engines/scsynth/attributes/0", \base, sidOffset, 0+gidOffset, 0, 0, true, true, \addToHead.asInt, false);
	chimconf.sendMsg("/engines/scsynth/attributes/1", \lead, sidOffset, 1+gidOffset, 1, 0, true, true, \addToHead.asInt, false);

	s.sendMsg('/g_new', 0+gidOffset, \addToHead.asInt, 0);
	s.sendMsg('/g_new', 1+gidOffset, \addToHead.asInt, 0);
	s.sync;

	chimconf.sendMsg("/sensors/number", {|msg|
		var n=msg[0];
		Routine.run({
			"./instruments2F.sc".load.value(n);
		}, clock:AppClock);
	});
})