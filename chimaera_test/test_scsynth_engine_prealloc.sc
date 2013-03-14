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

s.options.blockSize = 16;
s.latency = nil;
s.boot;

s.doWhenBooted({
	var inst, ev, txrx, chimconf;

	inst = SynthDef(\myinstrument, {|x, z, gate|
		var base, freq, sig;

		base = 24+12;
		freq = LinExp.kr(x, 0, 1, base.midicps, (base+48).midicps);	
		sig = SinOsc.ar(freq, mul:gate*z);

		OffsetOut.ar([0,1], sig);
	}).add;

	// preallocate 8 synths
	ev = (
		type: \on,
		instrument: \myinstrument,
		id: [1000, 1001, 1002, 1003, 1004, 1005, 1006, 1007],
		gate: 0
	).play;

	thisProcess.openUDPPort(4444);
	txrx = NetAddr ("chimaera.local", 4444);
	txrx.postln;

	chimconf = ChimaeraConf(s, txrx, txrx);

	chimconf.sendMsg("/chimaera/output/enabled", true); // enable output
	chimconf.sendMsg("/chimaera/output/address", "192.168.1.10:57110"); // send to scsynth port

	chimconf.sendMsg("/chimaera/scsynth/enabled", true); // enable scsynth output engine
	chimconf.sendMsg("/chimaera/scsynth/prealloc", true); // use prealloc mode of scsynth output engine
	chimconf.sendMsg("/chimaera/scsynth/offset", 1000); // offset of preallocated scsynth ids
	chimconf.sendMsg("/chimaera/scsynth/modulo", 8); // number of preallocated scsynth ids to cycle through

	chimconf.sendMsg("/chimaera/group/clear"); // clear groups
	chimconf.sendMsg("/chimaera/group/add", 1, ChimaeraConf.north, 0.0, 1.0); // add group
	chimconf.sendMsg("/chimaera/group/add", 2, ChimaeraConf.south, 0.0, 1.0); // add group
})