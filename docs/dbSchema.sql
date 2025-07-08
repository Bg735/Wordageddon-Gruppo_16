CREATE TABLE User(
                     name TEXT PRIMARY KEY,
                     password TEXT NOT NULL,
                     isAdmin BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE GameReport(
                           id INTEGER PRIMARY KEY AUTOINCREMENT,
                           user TEXT REFERENCES User(name) ON DELETE CASCADE NOT NULL ,
                           timestamp DATETIME DEFAULT  CURRENT_TIMESTAMP NOT NULL ,
                           difficulty TEXT NOT NULL CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
                           max_time TIME NOT NULL,
                           used_time TIME NOT NULL,
                           question_count INTEGER NOT NULL CHECK (question_count > 0),
                           score INTEGER NOT NULL CHECK (score >= 0),

                           CHECK (
                               max_time LIKE '__:__' AND
                               substr(max_time, 1, 2) BETWEEN '00' AND '60' AND
                               substr(max_time, 4, 2) BETWEEN '00' AND '59'
                               ),
                           CHECK (
                               used_time LIKE '__:__' AND
                               substr(used_time, 1, 2) BETWEEN '00' AND '60' AND
                               substr(used_time, 4, 2) BETWEEN '00' AND '59'
                               )
);

CREATE TABLE Document(
                         id INTEGER PRIMARY KEY AUTOINCREMENT,
                         title TEXT NOT NULL DEFAULT path,
                         path TEXT,
                         word_count INTEGER NOT NULL CHECK (word_count > 0)
);

CREATE TABLE Content(
                        document INTEGER REFERENCES Document(id) ON DELETE CASCADE NOT NULL,
                        report INTEGER REFERENCES GameReport(id) ON DELETE CASCADE NOT NULL,
                        PRIMARY KEY (document, report)
);

CREATE TABLE WDM(
                    document INTEGER REFERENCES Document(id) ON DELETE CASCADE,
                    word TEXT NOT NULL CHECK (LENGTH(word) > 0),
                    occurrences INTEGER NOT NULL CHECK (occurrences >= 0),
                    PRIMARY KEY (document, word)
);

CREATE TABLE StopWord(
                         word TEXT PRIMARY KEY CHECK (LENGTH(word) > 0)
);


-- Trigger su DELETE: blocca la cancellazione dell’ultimo admin
CREATE TRIGGER ensure_one_admin_before_delete
    BEFORE DELETE ON "User"
    FOR EACH ROW
    WHEN OLD.isAdmin = 1
        AND (SELECT COUNT(*) FROM "User" WHERE isAdmin = 1) = 1
BEGIN
    SELECT RAISE(ABORT, 'Deve esistere almeno un utente admin');
END;

-- Trigger su UPDATE: blocca l’aggiornamento dell’ultimo admin a non-admin
CREATE TRIGGER ensure_one_admin_before_update
    BEFORE UPDATE OF isAdmin ON "User"
    FOR EACH ROW
    WHEN OLD.isAdmin = 1
        AND NEW.isAdmin = 0
        AND (SELECT COUNT(*) FROM "User" WHERE isAdmin = 1) = 1
BEGIN
    SELECT RAISE(ABORT, 'Deve esistere almeno un utente admin');
END;