package bj.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import bj.test.BasicStrategy00;
import bj.test.BasicStrategy01;
import bj.test.BasicStrategy02;
import bj.test.BasicStrategy03;
import bj.test.BasicStrategy04;
import bj.test.BasicStrategy05;
import bj.test.BasicStrategy06;
import bj.test.BasicStrategy07;
import bj.test.BasicStrategy08;
import bj.test.BasicStrategy09;
import bj.test.BasicStrategy10;
import bj.test.BasicStrategy11;
import bj.test.BasicStrategy12;

@RunWith(Suite.class)
@SuiteClasses({ BasicStrategy00.class, BasicStrategy01.class,
		BasicStrategy02.class, BasicStrategy03.class, BasicStrategy04.class,
		BasicStrategy05.class, BasicStrategy06.class, BasicStrategy07.class,
		BasicStrategy08.class, BasicStrategy09.class, BasicStrategy10.class,
		BasicStrategy11.class, BasicStrategy12.class })
public class BasicStrategySuite {

}
