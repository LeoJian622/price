package uk.me.candle.eve.pricing.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Niklas
 */
public class SplitList {
    private final List<Integer> fullList;
    private List<List<Integer>> split;
    private List<Integer> last;
    private boolean done;
    private int index;
    private int size;
    private int lastSize;
    private double splitSize;

    public SplitList(Collection<Integer> fullList) {
        this.fullList = new ArrayList<>(fullList);
        size = fullList.size();
        lastSize = 0;
        index = 0;
        splitSize = 2;
        done = false;
    }

    public List<Integer> getNextList() {
        if (fullList.isEmpty() || !split()) {
            return null;
        }
        last = split.get(index);
        index++;
        return last;
    }

    private boolean split() {
        if (split == null || index >= split.size()) { //New split
            if (done) { //We are done here
                return false;
            }
            index = 0;
            if (lastSize == fullList.size()) { //Nothing Removed - skip a step
                //Exponential search
                splitSize = splitSize * splitSize;
                //Binary search
                //splitSize = 2;
            }
            //No search
            //size = 2;
            //Search
            size = (int) Math.ceil(size / splitSize);
            //Make lists
            split = split(fullList, size);
            if (split.size() == fullList.size()) {
                done = true;
            }
            lastSize = fullList.size();
        }
        return true;
    }

    public void removeLast() {
        if (last != null) {
            fullList.removeAll(last);
            size = fullList.size();
        }
    }

    public List<Integer> getFullList() {
        return new ArrayList<>(fullList);
    }

    private List<List<Integer>> split(List<Integer> in, int partitionSize) {
        List<List<Integer>> partitions = new ArrayList<>();
        for (int i = 0; i < in.size(); i += partitionSize) {
            partitions.add(new ArrayList<>(in.subList(i, i + Math.min(partitionSize, in.size() - i))));
        }
        return partitions;
    }
}
