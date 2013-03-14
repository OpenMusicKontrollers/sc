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

{
	var rx, tx, chimconf, chimtuio2, midio, lookup, baseID, leadID;

	thisProcess.openUDPPort(4444); // open port 4444 for listening to chimaera configuration replies
	tx = NetAddr ("chimaera.local", 4444);

	chimconf = ChimaeraConf(s, tx, tx);

	chimconf.sendMsg("/chimaera/output/enabled", true); // enable output
	chimconf.sendMsg("/chimaera/output/address", "192.168.1.10:3333"); // send output stream to port 3333

	chimconf.sendMsg("/chimaera/tuio/enabled", true); // enable Tuio output engine
	chimconf.sendMsg("/chimaera/tuio/long_header", false); // use short Tuio frame header (default)

	baseID = 0; // group 0 on chimaera device responds to everything and should not be overwritten
	leadID = 1;

	chimconf.sendMsg("/chimaera/group/clear"); // clear groups
	chimconf.sendMsg("/chimaera/group/set", baseID, \base, ChimaeraConf.north, 0.0, 1.0); // add group
	chimconf.sendMsg("/chimaera/group/set", leadID, \lead, ChimaeraConf.south, 0.0, 1.0); // add group

	thisProcess.openUDPPort(3333); // open port 3333 to listen for Tuio messages
	rx = NetAddr ("chimaera.local", 3333);
	chimtuio2 = ChimaeraTuio2(s, rx);

	MIDIClient.init;
	//midio = MIDIOut(0, MIDIClient.destinations[0].uid); // use this on MacOS, Windows to connect to the MIDI stream of choice
	midio = MIDIOut(0); // use this on Linux, as patching is usually done via ALSA/JACK
	midio.latency = 0; // send MIDI with no delay, instantaneously

	lookup = Dictionary.new; // lookup table of currently active keys

	chimtuio2.on = { |sid, pid, gid, x, z| // set callback function for blob on-events
		var midikey = x*48+48;

		lookup[sid] = midikey.round;
		midio.noteOn(gid, lookup[sid], 0x7f); // we're using the group id (gid) as MIDI channel number
		midio.bend(gid, midikey-lookup[sid]/48*0x2000+0x2000); // we're using a pitchbend span of 4800 cents
		midio.control(gid, 0x4a, z*0x7f); // sound controller #5, change this to volume, modulation, after-touch, ...
	};

	chimtuio2.off = { |sid, pid, gid| // set callback function for blob off-events
		midio.noteOff(gid, lookup[sid], 0x00);
		lookup[sid] = nil;
	};

	chimtuio2.set = { |sid, pid, gid, x, z| // set callback function for blob set-events
		var midikey = x*48+48;

		midio.bend(gid, midikey-lookup[sid]/48*0x2000+0x2000);
		midio.control(gid, 0x4a, z*0x7f); // sound controller #5
	};
}.value;
