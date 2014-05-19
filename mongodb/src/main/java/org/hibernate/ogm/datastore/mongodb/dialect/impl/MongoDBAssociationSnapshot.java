/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.dialect.impl;

import static org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoHelpers.getAssociationFieldOrNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoDBTupleSnapshot.SnapshotType;
import org.hibernate.ogm.datastore.spi.AssociationSnapshot;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.RowKey;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author Alan Fitton <alan at eth0.org.uk>
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class MongoDBAssociationSnapshot implements AssociationSnapshot {

	private final Map<RowKey, DBObject> map;
	private final DBObject dbObject;
	private final AssociationKey associationKey;
	private final AssociationStorageStrategy storageStrategy;

	/**
	 * @param document DBObject containing the association information
	 * @param key
	 */
	public MongoDBAssociationSnapshot(DBObject document, AssociationKey key, AssociationStorageStrategy storageStrategy) {
		this.storageStrategy = storageStrategy;
		this.dbObject = document;
		this.map = new LinkedHashMap<RowKey, DBObject>();
		this.associationKey = key;
		//for each element in the association property
		for ( DBObject row : getRows() ) {
			DBObject mongodbColumnData = row;
			Collection<String> columnNames = Arrays.asList( key.getRowKeyColumnNames() );

			//build data to construct the associated RowKey is column names and values
			List<Object> columnValues = new ArrayList<Object>();
			for ( String columnKey : columnNames ) {
				boolean getFromMongoData = true;
				int length = key.getColumnNames().length;
				// try and find the value in the key metadata
				for ( int index = 0 ; index < length; index++ ) {
					String assocColumn = key.getColumnNames()[index];
					if ( assocColumn.equals( columnKey ) ) {
						columnValues.add( associationKey.getColumnValues()[index] );
						getFromMongoData = false;
						break;
					}
				}
				//otherwise read it from the database structure
				if ( getFromMongoData == true ) {
					columnValues.add( mongodbColumnData.get( columnKey ) );
				}
			}
			RowKey rowKey = new RowKey(
					key.getTable(),
					columnNames.toArray( new String[columnNames.size()] ),
					columnValues.toArray() );
			//Stock database structure per RowKey
			this.map.put( rowKey, row );
		}
	}

	@Override
	public Tuple get(RowKey column) {
		DBObject row = this.map.get( column );
		return row == null ? null : new Tuple( new MongoDBTupleSnapshot( row, column, SnapshotType.SELECT ) );
	}

	//not for embedded
	public DBObject getQueryObject() {
		DBObject query = new BasicDBObject();
		query.put( MongoDBDialect.ID_FIELDNAME, dbObject.get( MongoDBDialect.ID_FIELDNAME ) );
		return query;
	}

	@Override
	public boolean containsKey(RowKey column) {
		return map.containsKey( column );
	}

	@Override
	public int size() {
		return map.size();
	}

	@SuppressWarnings("unchecked")
	private Collection<DBObject> getRows() {
		if ( storageStrategy == AssociationStorageStrategy.IN_ENTITY ) {
			return getAssociationFieldOrNull( associationKey, dbObject );
		}
		else {
			return (Collection<DBObject>) dbObject.get( MongoDBDialect.ROWS_FIELDNAME );
		}
	}

	public DBObject getRowKeyDBObject(RowKey rowKey) {
		return map.get( rowKey );
	}

	@Override
	public Set<RowKey> getRowKeys() {
		return map.keySet();
	}

	public DBObject getDBObject() {
		return this.dbObject;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "MongoDBAssociationSnapshot(" );
		sb.append( map.size() );
		sb.append( ") RowKey entries)." );
		return sb.toString();
	}
}