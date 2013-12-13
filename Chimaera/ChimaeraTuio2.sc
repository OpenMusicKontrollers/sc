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

ChimaeraTuio2 {
	classvar first_frame;
	var rx, frm, tok, alv, <>engine, blobs, old_blobs, last_fid, last_time, missing, ignore;

	*new {|s, iRx, iEngine|
		^super.new.init(s, iRx, iEngine);
	}

	*classInit {
		first_frame = 1;
	}

	initConn {|iRx, iEngine|
		rx = iRx;
		engine = iEngine;
		blobs = Order.new;
		old_blobs = Array.new;
		last_fid = 0;
		last_time = 0;
		missing = 0;
		ignore = false;

		// handling tuio messages
		frm = OSCFunc({|msg, time, addr, port|
			var fid, timestamp;

			fid = msg[1];
			timestamp = time; //msg[2]; // TODO sclang does not support the OSC timestamp as argument

			if(fid != (last_fid+1) ) {
				missing = missing + 1;
				["message missing", last_fid, fid, missing].postln;
			};

			ignore = false;
			if(timestamp < last_time) {
				["message late", timestamp, last_time].postln;
				ignore = true;
			};
	
			last_fid = fid;
			last_time = timestamp;

			if(engine.start.notNil) {engine.start.value(time)};

		}, "/tuio2/frm", rx);

		tok = OSCFunc({|msg, time, addr, port|
			var o, sid, pid, gid, x, z;

			if(ignore == false) {
				sid = msg[1];
				pid = msg[2] & 0xffff;
				gid = msg[3];
				x = msg[4];
				z = msg[5];
				//a = msg[6]; // not used

				blobs[sid] = [time, sid, pid, gid, x, z];
			};
		}, "/tuio2/tok", rx);

		alv = OSCFunc({|msg, time, addr, port|
			var n, tmp;

			if(ignore == false) {
				msg.removeAt(0); // remove /tuio2/alv

				n = msg.size;
				if(n==0) {
					if(engine.idle.notNil) {engine.idle.value(time);};
				};

				// search for disappeard blobs
				old_blobs do: {|v|
					if(msg.indexOf(v).isNil) {
						if(engine.off.notNil) {engine.off.valueArray(blobs[v])};
						blobs.removeAt(v);
					}
				};

				// search for new blobs
				msg do: {|v|
					if(old_blobs.indexOf(v).isNil) {
						if(engine.on.notNil) {engine.on.valueArray(blobs[v])};
					} {
						if(engine.set.notNil) {engine.set.valueArray(blobs[v])};
					};
				};

				if(engine.end.notNil) {engine.end.value(time)};

				old_blobs = msg;
			};
		}, "/tuio2/alv", rx);

		// engine.start {|time| };
		// engine.end {|time| };
		// engine.on {|time, sid, pid, gid, x, z| ["Tuio2 ON:", time, sid, pid, gid, x, z].postln;};
		// engine.off {|time, sid, pid, gid| ["Tuio2 OFF:", time, sid, pid, gid].postln;};
		// engine.set {|time, sid, pid, gid, x, z| ["Tuio2 SET:", time, sid, pid, gid, x, z].postln;};
		// engine.idle {|time| ["Tuio2 IDLE:", time].postln;};
	}

	init {|s, iRx, iCb|
		this.initConn(iRx, iCb);
	}
}
