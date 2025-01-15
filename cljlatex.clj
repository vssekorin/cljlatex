#!/usr/bin/env bb

(require '[babashka.fs :as fs]
         '[clojure.string :as str])

(when (empty? *command-line-args*)
  (println "Filename is necessary.")
  (System/exit 1))

(def in-file-name (first *command-line-args*))
(def out-file-name (str "result-" in-file-name))

(def start-symb "{clj>")
(def end-symb ">clj}")

(defn str-first [start [head & remaining]]
  (conj remaining (str start head)))

(defn str-last [end elements]
  (concat (drop-last elements) (list (str (last elements) end))))

(defn build-lines [start script end]
  (let [result (load-string script)]
    (->> (if (coll? result) result (list result))
         (map str)
         (str-first start)
         (str-last end))))

(def lines*
  (let [state (atom nil) start (atom nil) script (atom nil)]
    (for [line (fs/read-all-lines in-file-name)
          :let [start-position (str/index-of line start-symb)
                end-position (str/index-of line end-symb)]]
      (cond
        (and start-position end-position) (build-lines (subs line 0 start-position)
                                                       (subs line (+ (count start-symb) start-position) end-position)
                                                       (subs line (+ (count end-symb) end-position) (count line)))
        (and start-position (not end-position)) (do (reset! state :read-clj)
                                                    (reset! start (subs line 0 start-position))
                                                    (swap! script str (subs line (+ (count start-symb) start-position) (count line)))
                                                    nil)
        (and end-position (= @state :read-clj)) (do (reset! state nil)
                                                    (build-lines @start
                                                                 (str @script (subs line 0 end-position))
                                                                 (subs line (+ (count end-symb) end-position) (count line))))
        (= @state :read-clj) (do (swap! script str line)
                                 nil)
        :else line))))

(fs/delete-if-exists out-file-name)
(fs/create-file out-file-name)
(fs/write-lines out-file-name (->> lines* (filter some?) flatten))

(println "Done.")
