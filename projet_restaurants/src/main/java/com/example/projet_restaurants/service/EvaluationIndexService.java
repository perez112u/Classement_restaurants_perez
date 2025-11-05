package com.example.projet_restaurants.service;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EvaluationIndexService {

    // ecrit ou met a jour les documents dans l index
    private final IndexWriter indexWriter;

    // lit l'index
    private DirectoryReader directoryReader;

    // moteur de recherche
    private IndexSearcher indexSearcher;

    // transforme une chaine en requete
    private final QueryParser contentParser;

    public EvaluationIndexService () throws IOException {
        // ouvre ou cree un dossier local ou on va stocker l index
        String indexDirectoryPath = "index";
        Path path = Paths.get(indexDirectoryPath);
        Directory index = FSDirectory.open(path);

        // on prepare le parse sur le champ content
        StandardAnalyzer analyzer = new StandardAnalyzer();
        this.contentParser = new QueryParser("content", analyzer);

        // config du writer
        IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
        this.indexWriter = new IndexWriter(index, cfg);

        if (!DirectoryReader.indexExists(index)) {
            this.indexWriter.commit();
        }
        this.directoryReader = DirectoryReader.open(index);
        this.indexSearcher = new IndexSearcher(directoryReader);
    }

    // on doit recharger le reader si l'index a ete modifié
    private void refreshSearcher() {
        try {
            DirectoryReader newReader = DirectoryReader.openIfChanged(this.directoryReader);
            if (newReader != null) {
                this.directoryReader.close();
                this.directoryReader = newReader;
                this.indexSearcher = new IndexSearcher(this.directoryReader);
            }
        } catch (IOException e) {
            throw new RuntimeException("Lucene refresh failed", e);
        }
    }

    // enregister une evaluation dans l indexe lucene
    public void indexEvaluation(Long id, Long restaurantId, String evaluateur, String commentaire, Integer note) throws IOException {
        // le texte que lucene va indexer (nom de l evaluateur + le commentaire)
        StringBuilder content = new StringBuilder();
        if (evaluateur != null) content.append(evaluateur).append(' ');
        if (commentaire != null) content.append(commentaire);

        //
        Document doc = new Document();
        doc.add(new StringField("id", String.valueOf(id), Field.Store.YES));
        doc.add(new StringField("restaurantId", String.valueOf(restaurantId), Field.Store.NO));
        doc.add(new TextField("content", content.toString(), Field.Store.NO));
        if (note != null) doc.add(new StringField("note", String.valueOf(note), Field.Store.NO));

        this.indexWriter.updateDocument(new Term("id", String.valueOf(id)), doc);
        this.indexWriter.commit();
        // on refresh direct pour que ce soit a jour
        refreshSearcher();
    }

    // Supprimer une évaluation de l’index
    public void deleteEvaluation(Long evaluationId) {
        try {
            this.indexWriter.deleteDocuments(new Term("id", String.valueOf(evaluationId)));
            this.indexWriter.commit();
            refreshSearcher();
        } catch (IOException e) {
            throw new RuntimeException("Lucene delete failed", e);
        }
    }

    // recherche par mot clé + filtrer par restaurant
    public List<String> searchByKeywords(String q, Long restaurantId) {
        try {
            // on transforme les mots clé en requete
            Query textQuery = this.contentParser.parse(q);
            Query restoFilter = new TermQuery(new Term("restaurantId", String.valueOf(restaurantId)));

            BooleanQuery finalQuery = new BooleanQuery.Builder()
                    .add(textQuery, BooleanClause.Occur.MUST)   // doit matcher le texte
                    .add(restoFilter, BooleanClause.Occur.MUST)  // doit appartenir au resto
                    .build();

            // on execute la recherche et recupere les id des evals correspondante (10 eval max)
            TopDocs topDocs = this.indexSearcher.search(finalQuery, 10);
            return Arrays.stream(topDocs.scoreDocs)
                    .map(scoreDoc -> {
                        try {
                            return this.indexSearcher.storedFields().document(scoreDoc.doc).getField("id").stringValue();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }


}

