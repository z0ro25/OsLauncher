package com.amz.ios.http.Internal;

import java.util.Collection;
import java.util.Comparator;

/**
 * Author       : yizhihao
 * Create time  : 2016-12-12 下午3:26
 * email        : 562536056@qq.com || yizhihao.hut@gmail.com
 */
public class MerlinTreeList<T> extends TreeList<T> {

    private Comparator<T> mComparable;

    public MerlinTreeList(Comparator<T> comparable) {
        mComparable = comparable;
    }

    @Override
    public void add(int index, T right) {
        if (index > size() || index < 0)
            throw new IndexOutOfBoundsException("Invalid index:" + index + ", size=" + size());
        //no element
        if (index == 0 && size() == 0) {
            super.add(index, right);
            return;
        }
        if (index == size()) {
            index--;
            addToLeft(index, right);
            return;
        }
        final T current = get(index);
        final int startAction = mComparable.compare(current, right);
        if (startAction > 0) {
            index++;
            addToRight(index, right);
            return;
        } else if (startAction < 0) {
            index--;
            addToLeft(index, right);
            return;
        }
        super.add(index, right);
    }

    public void addToRight(int index, T left) {
        //first element
        final T right = get(index);
        if (index == size()) {
            super.add(index, left);
        } else {
            final int action = mComparable.compare(left, right);
            if (action > 0) {
                index++;
                addToRight(index, left);
                return;
            }
            super.add(index, left);
        }
    }

    @Override
    public boolean add(T object) {
        add(size(), object);
        return true;
    }

    public void addToLeft(int index, T right) {
        //first element
        if (index == -1) {
            super.add(0, right);
        } else {
            final T left = get(index);
            final int action = mComparable.compare(left, right);
            if (action < 0) {
                index--;
                addToLeft(index, right);
                return;
            }
            index++;
            super.add(index, right);
        }
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        for (T t : c) {
            add(t);
        }
        return true;
    }

    //    public static void main(String[] args) {
//        MerlinTreeList merlinTreeList = new MerlinTreeList(new Comparator<Integer>() {
//            @Override
//            public int compare(Integer lhs, Integer rhs) {
//                return lhs - rhs;
//            }
//        });
//
//        Random r = new Random();;
//        int result;
//        for (int i = 0; i < 10; i++) {
//            result = r.nextInt(100) + 1;
//            System.out.println(result);
//            merlinTreeList.add(result);
//        }
//        System.out.println(merlinTreeList);
//    }
}
