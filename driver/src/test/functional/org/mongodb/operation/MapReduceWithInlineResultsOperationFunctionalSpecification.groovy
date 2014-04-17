/*
 * Copyright (c) 2008-2014 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.operation

import org.bson.types.Code
import org.mongodb.AsyncBlock
import org.mongodb.Document
import org.mongodb.Fixture
import org.mongodb.FunctionalSpecification
import org.mongodb.MapReduceAsyncCursor
import org.mongodb.MapReduceCursor
import org.mongodb.MongoException
import org.mongodb.MongoFuture
import org.mongodb.ReadPreference
import org.mongodb.codecs.DocumentCodec
import org.mongodb.connection.SingleResultCallback
import org.mongodb.test.CollectionHelper

import static org.hamcrest.CoreMatchers.not
import static org.hamcrest.Matchers.hasKey
import static org.junit.Assume.assumeTrue
import static org.mongodb.Fixture.getSession
import static spock.util.matcher.HamcrestSupport.that

class MapReduceWithInlineResultsOperationFunctionalSpecification extends FunctionalSpecification {
    private final documentCodec = new DocumentCodec()
    def mapReduce = new MapReduce(new Code('function(){ emit( this.name , 1 ); }'),
                                  new Code('function(key, values){ return values.length; }'))
    def expectedResults = [['_id': 'Pete', 'value': 2.0] as Document,
                           ['_id': 'Sam', 'value': 1.0] as Document]

    def setup() {
        CollectionHelper<Document> helper = new CollectionHelper<Document>(documentCodec, getNamespace())
        Document pete = new Document('name', 'Pete').append('job', 'handyman')
        Document sam = new Document('name', 'Sam').append('job', 'plumber')
        Document pete2 = new Document('name', 'Pete').append('job', 'electrician')
        helper.insertDocuments(pete, sam, pete2)
    }


    def 'should return the correct results and the default command result is not verbose'() {
        given:
        def operation = new MapReduceWithInlineResultsOperation(namespace, mapReduce, documentCodec, ReadPreference.primary())

        when:
        MapReduceCursor<Document> results = operation.execute(getSession())

        then:
        results.iterator().toList() == expectedResults
        that results.getResponse(), not(hasKey('timing'))
    }

    def 'should return the correct results and the default command result is not verbose asynchronously'() {
        assumeTrue(Fixture.mongoClientURI.options.isAsyncEnabled())

        given:
        def operation = new MapReduceWithInlineResultsOperation(namespace, mapReduce, documentCodec, ReadPreference.primary())

        when:
        MongoFuture<MapReduceAsyncCursor> results = operation.executeAsync(getSession())
        def result = new SingleResultFuture<List<Document>>()
        results.register(new SingleResultCallback<MapReduceAsyncCursor<Document>>() {
            @Override
            void onResult(final MapReduceAsyncCursor<Document> cursor, final MongoException e) {
                cursor.start(new AsyncBlock<Document>() {
                    List<Document> docList = []

                    @Override
                    void done() {
                        result.init(docList, null)
                    }

                    @Override
                    void apply(final Document value) {
                        if (value != null) {
                            docList += value
                        }
                    }
                })
            }
        })

        then:
        result.get().iterator().toList() == expectedResults
        that results.get().getResponse(), not(hasKey('timing'))
    }

    def 'when verbose is set the command result should contain more information'() {
        given:
        def mapReduceVerbose = mapReduce.verbose()
        def operation = new MapReduceWithInlineResultsOperation(namespace, mapReduceVerbose, documentCodec, ReadPreference.primary())

        when:
        MapReduceCursor<Document> results = operation.execute(getSession())

        then:
        results.iterator().toList() == expectedResults
        that results.getResponse(), hasKey('timing')
    }

    def 'when verbose is set the command result should contain more information asynchronously'() {
        assumeTrue(Fixture.mongoClientURI.options.isAsyncEnabled())

        given:
        def mapReduceVerbose = mapReduce.verbose()
        def operation = new MapReduceWithInlineResultsOperation(namespace, mapReduceVerbose, documentCodec, ReadPreference.primary())

        when:
        MongoFuture<MapReduceAsyncCursor> results = operation.executeAsync(getSession())
        def result = new SingleResultFuture<List<Document>>()
        results.register(new SingleResultCallback<MapReduceAsyncCursor<Document>>() {
            @Override
            void onResult(final MapReduceAsyncCursor<Document> cursor, final MongoException e) {
                cursor.start(new AsyncBlock<Document>() {
                    List<Document> docList = []

                    @Override
                    void done() {
                        result.init(docList, null)
                    }

                    @Override
                    void apply(final Document value) {
                        if (value != null) {
                            docList += value
                        }
                    }
                })
            }
        })


        then:
        result.get().iterator().toList() == expectedResults
        that results.get().getResponse(), hasKey('timing')
    }

}