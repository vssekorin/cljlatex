#!/usr/bin/env bb

(require '[babashka.fs :as fs]
         '[clojure.string :as str])

(when (empty? *command-line-args*)
  (println "Filename is necessary.")
  (System/exit 1))

(def in-file-name (first *command-line-args*))
(def out-file-name (str "result-" in-file-name))

(def start-symb "{clj>")
(def start-before-symb "{clj:before>")
(def end-symb ">clj}")

(def before-form (atom nil))

(defn str-first [start [head & remaining]]
  (conj remaining (str start head)))

(defn str-last [end elements]
  (concat (drop-last elements) (list (str (last elements) end))))

(defn build-lines [start form end]
  (let [result (load-string (str "(do " @before-form form ")"))]
    (->> (if (coll? result) result (list result))
         (map str)
         (str-first start)
         (str-last end))))

(def lines*
  (let [state (atom nil) start (atom nil) form (atom nil)]
    (for [line (fs/read-all-lines in-file-name)
          :let [start-position (str/index-of line start-symb)
                start-before-position (str/index-of line start-before-symb)
                end-position (str/index-of line end-symb)]]
      (cond
        (and start-position end-position) (build-lines (subs line 0 start-position)
                                                       (subs line (+ (count start-symb) start-position) end-position)
                                                       (subs line (+ (count end-symb) end-position) (count line)))
        (and start-before-position end-position) (do (reset! before-form (subs line (+ (count start-before-symb) start-before-position) end-position))
                                                     (str (subs line 0 start-before-position) (subs line (+ (count end-symb) end-position) (count line))))
        start-position (do (reset! state :read-clj)
                           (reset! start (subs line 0 start-position))
                           (swap! form str (subs line (+ (count start-symb) start-position) (count line)))
                           nil)
        start-before-position (do (reset! state :read-clj-before)
                                  (reset! before-form (subs line (+ (count start-before-symb) start-before-position) (count line)))
                                  (subs line 0 start-before-position))
        (and end-position (= @state :read-clj)) (do (reset! state nil)
                                                    (build-lines @start
                                                                 (str @form (subs line 0 end-position))
                                                                 (subs line (+ (count end-symb) end-position) (count line))))
        (and end-position (= @state :read-clj-before)) (do (reset! state nil)
                                                           (swap! before-form str (subs line 0 end-position))
                                                           (subs line (+ (count end-symb) end-position) (count line)))
        (= @state :read-clj) (do (swap! form str line)
                                 nil)
        (= @state :read-clj-before) (do (swap! before-form str line)
                                        nil)
        :else line))))

(fs/delete-if-exists out-file-name)
(fs/create-file out-file-name)
(fs/write-lines out-file-name (->> lines* (filter some?) flatten))

(println "Done.")
