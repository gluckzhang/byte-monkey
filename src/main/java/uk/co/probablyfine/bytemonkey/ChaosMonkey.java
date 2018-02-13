package uk.co.probablyfine.bytemonkey;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeoutException;

public class ChaosMonkey {
    public static void doChaos(String tcIndexInfo, String tcType, String memcachedHost, int memcachedPort, String defaultMode) throws Throwable {
        String chaosMode = getMode(tcIndexInfo, memcachedHost, memcachedPort);
        if (chaosMode == null) {
            System.out.println("INFO ByteMonkey unregistered try catch found");
            return;
        } else if (chaosMode.equals("analyze")) {
            printInfo(tcIndexInfo);
        } else if (chaosMode.equals("inject")) {
            throw throwOrDefault(tcIndexInfo, tcType);
        }
    }

    public static String getMode(String tcIndexInfo, String memcachedHost, int memcachedPort) {
        String mode = null;

        try {
            MemcachedClient client = new XMemcachedClient(memcachedHost,memcachedPort);
            mode = client.get(tcIndexInfo, 1000);
            client.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (MemcachedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            String executedClassName = Thread.currentThread().getStackTrace()[3].getClassName();
            String executedMethodName = Thread.currentThread().getStackTrace()[3].getMethodName();
            System.out.println(String.format("INFO ByteMonkey getMode time out, %s @ %s", executedMethodName, executedClassName));
        }

        return mode;
    }

    public static void printInfo(String tcIndexInfo) {
        String executedClassName = Thread.currentThread().getStackTrace()[3].getClassName();
        String executedMethodName = Thread.currentThread().getStackTrace()[3].getMethodName();

        // TryCatch Info
        System.out.println(String.format("INFO ByteMonkey try catch index %s, %s @ %s", tcIndexInfo, executedMethodName, executedClassName));
        System.out.println("----");
    }

    public static Throwable throwOrDefault(String tcIndexInfo, String tcType) {
        String executedClassName = Thread.currentThread().getStackTrace()[3].getClassName();
        String executedMethodName = Thread.currentThread().getStackTrace()[3].getMethodName();

        // TryCatch Injection Info
        System.out.println(String.format("INFO ByteMonkey injection! %s, %s @ %s", tcIndexInfo, executedMethodName, executedClassName));
        System.out.println("INFO ByteMonkey StackTrace Info:");
        new Throwable().printStackTrace();

        String dotSeparatedClassName = tcType.replace("/", ".");
        try {
            Class<?> p = Class.forName(dotSeparatedClassName, false, ClassLoader.getSystemClassLoader());

            if (Throwable.class.isAssignableFrom(p)) {
                return (Throwable) p.newInstance();
            } else {
                return new ByteMonkeyException(tcType);
            }
        } catch (IllegalAccessException e) {
            return new ByteMonkeyException(tcType);
        } catch (Exception e) {
            return new RuntimeException(tcType);
        }
    }

    public static void registerTrycatchInfo(AgentArguments arguments, String memcachedKey, String value) {
        // register to a csv file
        File csvFile = new File(arguments.csvfilepath());
        try {
            PrintWriter out = null;
            if (csvFile.exists()) {
                out = new PrintWriter(new FileWriter(csvFile, true));
                out.println(String.format("%s,%s,%s", memcachedKey, "no", value));
            } else {
                csvFile.createNewFile();
                out = new PrintWriter(new FileWriter(csvFile));
                out.println("tcIndex,methodName,className,isCovered,mode");
                out.println(String.format("%s,%s,%s", memcachedKey, "no", value));
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // register to memcached server
        // lots of timeout issues so we only do the file registeration first
        // then the controller will register all the info in memcached server
        /*
        try {
            MemcachedClient client = new XMemcachedClient(arguments.memcachedHost(), arguments.memcachedPort());
            client.set(memcachedKey, 0, value);
            client.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (MemcachedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            System.out.println(String.format("INFO ByteMonkey registerTrycatchInfo time out (%s)", memcachedKey));
        }
        */
    }
}