package top.zibin.luban;

/**
 * Created by roy on 2017/4/25.
 */

public class ThreadPoolFactory {

    static ThreadPoolProxy mNormalPool;
    static ThreadPoolProxy mDownLoadPool;

    /**
     * 得到一个普通的线程池
     *
     * @return
     */
    public static ThreadPoolProxy getNormalPool() {
        if (mNormalPool == null) {
            synchronized (ThreadPoolFactory.class) {
                if (mNormalPool == null) {
                    mNormalPool = new ThreadPoolProxy(5, 5, 3000);
                }
            }
        }
        return mNormalPool;
    }
}
