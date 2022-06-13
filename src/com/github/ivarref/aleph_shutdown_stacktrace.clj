(ns com.github.ivarref.aleph-shutdown-stacktrace
  (:require [aleph.http :as http]
            [clojure.tools.logging :as log])
  (:import (java.io Closeable)
           (java.net InetSocketAddress)
           (java.util.concurrent Executors)
           (sun.misc Signal SignalHandler))
  (:gen-class))


(defn handler [_]
  {:status  200
   :headers {"content-type" "text/plain"}
   :body    "Hello World!\n"})

(defonce servers (atom []))

(defn stop! [curr]
  (doseq [inst curr]
    (when (and inst (instance? Closeable inst))
      (try
        (.close ^Closeable inst)
        (log/info "OK shutdown server")
        (catch Throwable t
          (log/warn "Could not shutdown:" (ex-message t))))))
  [])

(defn run-main [_]
  (swap! servers
         (fn [curr]
           (stop! curr)
           [(http/start-server (fn [req] (handler req))
                               {:executor       (Executors/newFixedThreadPool 256)
                                :socket-address (InetSocketAddress. "0.0.0.0" 8080)})]))
  (log/info "Server listening at 0.0.0.0:8080"))

(defn shutdown! [p]
  (swap! servers stop!)
  (shutdown-agents)
  (deliver p nil))

(defn -main
  "Main method used to start the system from a JAR file."
  [& _args]
  (let [p (promise)]
    (Signal/handle (Signal. "INT")
                   (reify SignalHandler
                     (handle [_ _]
                       (log/info "Received SIGINT")
                       (shutdown! p))))
    (future
      (log/info "shutting down in 30 seconds ...")
      (when (= :timeout (deref p 10000 :timeout))
        (shutdown! p)))
    (run-main nil)
    @p
    (log/info "About to exit")))
