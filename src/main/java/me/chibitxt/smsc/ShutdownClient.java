package me.chibitxt.smsc;

import java.util.concurrent.ExecutorService;
import java.util.Map;
import java.util.Iterator;
import com.cloudhopper.commons.util.*;

public class ShutdownClient implements Runnable {
  private ExecutorService executorService;
  private Map<String,LoadBalancedList<OutboundClient>> balancedLists;
  private net.greghaines.jesque.worker.Worker jedisWorker;
  private net.greghaines.jesque.client.Client jedisClient;

  public ShutdownClient(ExecutorService executorService, Map balancedLists, net.greghaines.jesque.worker.Worker jedisWorker, net.greghaines.jesque.client.Client jedisClient) {
    this.executorService = executorService;
    this.balancedLists = balancedLists;
    this.jedisWorker = jedisWorker;
    this.jedisClient = jedisClient;
  }

  public void run() {
    jedisWorker.end(true);
    jedisClient.end();

    executorService.shutdownNow();
    ReconnectionDaemon.getInstance().shutdown();

    Iterator it = balancedLists.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry pair = (Map.Entry)it.next();
      LoadBalancedList<OutboundClient> list = (LoadBalancedList<OutboundClient>)pair.getValue();
      for (LoadBalancedList.Node<OutboundClient> node : list.getValues()) {
        OutboundClient value = node.getValue();
        value.shutdown();
      }
      it.remove(); // avoids a ConcurrentModificationException
    }
  }
}
