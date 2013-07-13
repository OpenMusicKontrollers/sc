#!/usr/bin/sclang

/*
 * Copyright (c) 2012-2013 Hanspeter Portner (agenthp@users.sf.net)
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

Server.supernova;
s.options.blockSize = 0x10;
s.options.memSize = 0x10000;
s.latency = nil;
s.boot;

s.doWhenBooted({
	var inst, txrx, chimconf, instruments, toneID, bowID, busX, busY;

	thisProcess.openUDPPort(4444);
	txrx = NetAddr ("chimaera.local", 4444);

	chimconf = ChimaeraConf(s, txrx, txrx);

	chimconf.sendMsg("/chimaera/output/enabled", true); // enable output socket on device
	chimconf.sendMsg("/chimaera/output/address", "192.168.1.10:57110"); // send to scsynth port
	chimconf.sendMsg("/chimaera/output/offset", 0.001); // add 1ms offset to bundle timestamps
	chimconf.sendMsg("/chimaera/output/reset"); // reset all output engines

	chimconf.sendMsg("/chimaera/interpolation/order", 3); // cubic interpolation

	chimconf.sendMsg("/chimaera/scsynth/enabled", true); // enable scsynth output engine
	chimconf.sendMsg("/chimaera/scsynth/offset", 1000); // offset of new synthdef ids
	chimconf.sendMsg("/chimaera/scsynth/modulo", 8000); // modulo of new synthdef ids
	// id numbers on device will cycle linearly from offset to (offset+modulo) circularly

	bowID = 0;
	toneID = 1;

	busX = Bus.control(s, 1);
	busY = Bus.control(s, 1);

	"../templates/single_group.sc".load.value(toneID);
	"../instruments/cello_bow.sc".load.value(\bow, busX, busY);
	"../instruments/cello_tone.sc".load.value(\tone, busX, busY);

	chimconf.sendMsg("/chimaera/group/clear"); // clear groups
	chimconf.sendMsg("/chimaera/group/set", bowID, \bow, ChimaeraConf.south, 0.0, 1.0); // add group
	chimconf.sendMsg("/chimaera/group/set", toneID, \tone, ChimaeraConf.north, 0.0, 1.0); // add group
});