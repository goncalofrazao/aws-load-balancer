package pt.ulisboa.tecnico.cnv.javassist.tools;

import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import javassist.CannotCompileException;

public class ICount extends AbstractJavassistTool {
    private static Map<Long, Long> nblocks_per_thread = new HashMap<Long, Long>();
    private static Map<Long, Long> ninsts_per_thread = new HashMap<Long, Long>();

    public ICount(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    public static void resetStatistics() {
        long thread_id = Thread.currentThread().getId();
        ninsts_per_thread.remove(thread_id);
        nblocks_per_thread.remove(thread_id);
    }

    public static long getInsts() {
        return ninsts_per_thread.get(Thread.currentThread().getId());
    }

    public static long getBlocks() {
        return nblocks_per_thread.get(Thread.currentThread().getId());
    }

    public static void incBasicBlock(int position, int length) {
        long thread_id = Thread.currentThread().getId();
        
        if (!nblocks_per_thread.containsKey(thread_id)) {
            nblocks_per_thread.put(thread_id, 0L);
        }
        
        if (!ninsts_per_thread.containsKey(thread_id)) {
            ninsts_per_thread.put(thread_id, 0L);
        }
        
        nblocks_per_thread.put(thread_id, nblocks_per_thread.get(thread_id) + 1);
        ninsts_per_thread.put(thread_id, ninsts_per_thread.get(thread_id) + length);
    }

    public static void logFile(String filename, String method) {
        File file = new File(filename);
        try {
            FileWriter filewriter = new FileWriter(file, true);
            filewriter.write(method + "\n");

            filewriter.write("ICount\n");
            filewriter.write(String.format("%d %d\n", getBlocks(), getInsts()));

            filewriter.write("\n");
            filewriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        block.behavior.insertAt(block.line, String.format("%s.incBasicBlock(%s, %s);", ICount.class.getName(), block.getPosition(), block.getLength()));
    }

}
