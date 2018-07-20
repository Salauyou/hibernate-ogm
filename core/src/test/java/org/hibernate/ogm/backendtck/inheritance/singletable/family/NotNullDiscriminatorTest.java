/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.inheritance.singletable.family;

import java.util.List;

import static java.util.stream.Collectors.toList;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import static org.fest.assertions.Assertions.assertThat;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.hibernate.search.annotations.Indexed;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class NotNullDiscriminatorTest extends OgmJpaTestCase {

	private Alien nibbler = new Alien( "Lord Nibbler", "NIBBLONIAN" );
	private Alien bender = new Alien( "Bender Rodrigues", "ROBOT" );

	private EntityManager em;

	@Before
	public void setUp() {
		em = getFactory().createEntityManager();
		em.getTransaction().begin();
		em.persist( nibbler );
		em.persist( bender );
		em.flush();
		em.clear();
		em.getTransaction().commit();
	}

	@After
	public void tearDown() {
		try {
			em.getTransaction().begin();
			em.remove( em.merge( nibbler ) );
			em.remove( em.merge( bender ) );
			em.getTransaction().commit();
		}
		finally {
			em.close();
		}
	}


	@Test
	public void testGetOneWithNotNullDiscriminator() {
		Person person = em.find( Person.class, bender.getName() );
		assertThat( person ).isInstanceOf( Alien.class );
		assertThat( person.getName() ).isEqualTo( bender.getName() );
		assertThat( ( (Alien) person ).type ).isEqualTo( bender.type );
	}


	@Test
	public void testGetAllWithNotNullDiscriminator() {
		List<Person> all = em.createQuery( "FROM Person p", Person.class ).getResultList();
		assertThat( all ).hasSize( 2 );
		assertThat( all.stream().map( Person::getName ).collect( toList() ) )
			.containsOnly( nibbler.getName(), bender.getName() );
		assertThat( all.stream().map( p -> ( (Alien) p ).getType() ).collect( toList() ) )
			.containsOnly( nibbler.getType(), bender.getType() );
	}


	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ Family.class, Person.class, Alien.class };
	}


	@Entity
	@DiscriminatorValue("not null")
	@Indexed
	static class Alien extends Person {

		@Column(name = "TYPE")
		private String type;

		public Alien() {
		}

		public Alien(String name, String type) {
			super( name );
			this.type = type;
		}

		public String getType() {
			return type;
		}
	}

}
