package de.swa.mmfg;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by Patrick Steinert on 04.05.23.
 */
public class TimerangeTest {
	@Test
	public void testIsSmaller() {
		Timerange comparable = new Timerange();
		Date start = new Date();
		start.setHours(1);
		start.setMinutes(10);
		start.setSeconds(30);

		Date end = Date.from(start.toInstant());
		end.setHours(2);

		comparable.setBegin(start);
		comparable.setEnd(end);

		Timerange toCompare = new Timerange();
		Date start2 = new Date();
		start2.setHours(1);
		start2.setMinutes(20);
		start2.setSeconds(00);

		Date end2 = Date.from(start2.toInstant());
		end2.setMinutes(30);

		toCompare.setBegin(start2);
		toCompare.setEnd(start2);

		assertTrue(comparable.isSmaller(toCompare));

	}

	@Test
	public void testIsGreater() {
		Timerange comparable = new Timerange();
		Date start = new Date();
		start.setHours(1);
		start.setMinutes(10);
		start.setSeconds(30);

		Date end = Date.from(start.toInstant());
		end.setHours(2);

		comparable.setBegin(start);
		comparable.setEnd(end);

		Timerange toCompare = new Timerange();
		Date start2 = new Date();
		start2.setHours(1);
		start2.setMinutes(20);
		start2.setSeconds(00);

		Date end2 = Date.from(start2.toInstant());
		end2.setMinutes(30);

		toCompare.setBegin(start2);
		toCompare.setEnd(end2);

		assertTrue(comparable.isGreater(toCompare));

		end2.setHours(3);
		assertFalse(comparable.isGreater(toCompare));

	}

	@Test
	public void testIsInside() {
		Timerange comparable = new Timerange();
		Date start = new Date();
		start.setHours(1);
		start.setMinutes(10);
		start.setSeconds(30);

		Date end = Date.from(start.toInstant());
		end.setHours(2);

		comparable.setBegin(start);
		comparable.setEnd(end);

		Timerange toCompare = new Timerange();
		Date start2 = new Date();
		start2.setHours(1);
		start2.setMinutes(20);
		start2.setSeconds(00);

		Date end2 = Date.from(start2.toInstant());
		end2.setMinutes(30);

		toCompare.setBegin(start2);
		toCompare.setEnd(end2);

		assertTrue(comparable.isInside(toCompare));

		end2.setHours(3);
		assertFalse(comparable.isInside(toCompare));

	}

	@Test
public void testINotsInside2() {
		Timerange comparable = new Timerange();
		Date start = new Date();
		start.setHours(1);
		start.setMinutes(10);
		start.setSeconds(30);

		Date end = Date.from(start.toInstant());
		end.setHours(2);

		comparable.setBegin(start);
		comparable.setEnd(end);

		Timerange toCompare = new Timerange();
		Date start2 = new Date();
		start2.setHours(2);
		start2.setMinutes(20);
		start2.setSeconds(00);

		Date end2 = Date.from(start2.toInstant());
		end2.setMinutes(30);

		toCompare.setBegin(start2);
		toCompare.setEnd(end2);

		assertFalse(comparable.isInside(toCompare));

	}

	// Test for NullPointerException
	@Test
	public void testNullPointerException() {
		Timerange comparable = new Timerange();
		Date start = new Date();
		start.setHours(1);
		start.setMinutes(10);
		start.setSeconds(30);

		Date end = Date.from(start.toInstant());
		end.setHours(2);
		assertFalse(comparable.isInside(null));


		comparable.setBegin(start);
		comparable.setEnd(end);

		Timerange toCompare = new Timerange();
		
		assertFalse(comparable.isInside(toCompare));

	}
}
