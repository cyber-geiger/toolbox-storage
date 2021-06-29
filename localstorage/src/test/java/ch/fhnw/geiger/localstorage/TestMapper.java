package ch.fhnw.geiger.localstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import ch.fhnw.geiger.localstorage.db.GenericController;
import ch.fhnw.geiger.localstorage.db.StorageMapper;
import ch.fhnw.geiger.localstorage.db.data.Node;
import ch.fhnw.geiger.localstorage.db.data.NodeImpl;
import ch.fhnw.geiger.localstorage.db.data.NodeValue;
import ch.fhnw.geiger.localstorage.db.data.NodeValueImpl;
import ch.fhnw.geiger.localstorage.db.mapper.DummyMapper;
import ch.fhnw.geiger.localstorage.db.mapper.H2SqlMapper;
import ch.fhnw.geiger.totalcross.Locale;
import java.io.File;
import java.util.List;
import java.util.Vector;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/***
 * <p>Test the apper capabilities.</p>
 */
public class TestMapper {

  private static final List<StorageMapper> mapperList = new Vector<>();

  /**
   * SetUp clean in memory db before each Test.
   */
  @BeforeClass
  public static void setupClass() throws StorageException {
    // First test impplementation on dummy mapper
    mapperList.add(new DummyMapper());
    mapperList.add(new H2SqlMapper("jdbc:h2:./testdb;AUTO_SERVER=TRUE", "sa2", "1234"));
  }

  /**
   * Destroy in memory db after every test.
   *
   * @throws StorageException in case of any problems
   */
  @AfterClass
  public static void tearDownClass() throws StorageException {
    for (StorageMapper mapper : mapperList) {
      mapper.close();
      new File("./testdb.mv.db").delete();
    }
  }

  /**
   * <p>Create test environment.</p>
   */
  @Before
  public void setupTest() throws StorageException {
    // clear all mappers
    for (StorageMapper mapper : mapperList) {
      mapper.zap();
    }
  }

  /**
   * Ter down test environment (and make sure that all remanences are cleaned).
   */
  @After
  public void tearDownTest() {
    // write data to all mappers
    for (StorageMapper mapper : mapperList) {
      mapper.flush();
    }
  }

  @Test
  public void testAddRootNode() throws StorageException {
    for (StorageMapper mapper : mapperList) {
      System.out.println("## Testing mapper " + mapper + " in " + (new Object() {
      }).getClass().getEnclosingMethod().getName());
      NodeImpl node = new NodeImpl("testNode1", "");

      mapper.add(node);

      // make sure that lastupdated and owner is updated upon add
      mapper.get(":testNode1");
    }
  }

  @Test
  public void testAddRootNodeNull() throws StorageException {
    for (StorageMapper mapper : mapperList) {
      System.out.println("## Testing mapper " + mapper + " in " + (new Object() {
      }).getClass().getEnclosingMethod().getName());
      NodeImpl node = new NodeImpl("testNode1");

      mapper.add(node);

      // make sure that lastupdated and owner is updated upon add
      mapper.get(":testNode1");
    }
  }

  @Test
  public void testAddRootNodeIllegalName() {
    for (StorageMapper mapper : mapperList) {
      for (String name : new String[]{":name", "na:me", "name:"}) {
        System.out.println("## Testing mapper " + mapper + " in " + (new Object() {
        }).getClass().getEnclosingMethod().getName());
        NodeImpl node = new NodeImpl(name, "");
        try {
          mapper.add(node);
          fail("duplicate adding unexpectedly successful");
        } catch (StorageException e) {
          // this should fail
          System.out.println("#### adding with name " + name
              + " failed as expected with exception " + e);
        }

        // make sure that the element was not added
        try {
          Node n = mapper.get(name);
          fail("adding with illegal name was retrieveaeable");
        } catch (StorageException e) {
          // this should fail
          System.out.println("#### node with name " + name
              + " was not stored as expected with exception " + e);
        }
      }
    }
  }

  @Test
  public void testAddRootNodeIllegalParent() throws StorageException {
    for (StorageMapper mapper : mapperList) {
      mapper.add(new NodeImpl("na", ""));
      for (String parent : new String[]{":name:", "na:me", "name:", "::", ":na::"}) {
        System.out.println("## Testing mapper " + mapper + " in " + (new Object() {
        }).getClass().getEnclosingMethod().getName());
        NodeImpl node = new NodeImpl("name", parent);
        try {
          mapper.add(node);
          fail("duplicate adding unexpectedly successful");
        } catch (StorageException e) {
          // this should fail
          System.out.println("#### adding with parent " + parent
              + " failed as expected with exception " + e);
        }

        // make sure that the element was not added
        try {
          mapper.get(parent);
          fail("adding with illegal name was retrieveaeable");
        } catch (StorageException e) {
          // this should fail
          System.out.println("#### node with parent " + parent
              + " was not stored as expected with exception " + e);
        }
      }
    }
  }

  @Test
  public void testDuplicateAddNode() throws StorageException {
    for (StorageMapper mapper : mapperList) {
      System.out.println("## Testing mapper " + mapper + " in " + (new Object() {
      }).getClass().getEnclosingMethod().getName());
      NodeImpl node = new NodeImpl("testNode1", "");
      mapper.add(node);
      try {
        mapper.add(node);
        fail("duplicate adding unexpecetedly successful");
      } catch (StorageException e) {
        // this should fail
      }

      // make sure that lastupdated and owner is updated upon add
      mapper.get(":testNode1");
    }
  }

  @Test
  public void testAddNodeWithoutParent() {
    for (StorageMapper mapper : mapperList) {
      System.out.println("## Testing mapper " + mapper + " in " + (new Object() {
      }).getClass().getEnclosingMethod().getName());
      NodeImpl node = new NodeImpl("testNode1", ":notAValidParent");

      try {
        mapper.add(node);
        fail("unexpectedly successfully added a node without a parent");
      } catch (StorageException e) {
        // this should fail
      }
    }
  }

  @Test
  public void testGetNode() throws StorageException {
    for (StorageMapper mapper : mapperList) {
      System.out.println("## Testing mapper " + mapper + " in " + (new Object() {
      }).getClass().getEnclosingMethod().getName());
      NodeImpl node = new NodeImpl("testNode1", "");
      NodeValue nv = new NodeValueImpl("key", "value", "type", "description", 1);
      node.addValue(nv);
      NodeImpl childNode = new NodeImpl("testNode1a", ":testNode1");

      // write data
      NodeImpl node2 = new NodeImpl("testNode2", "");
      mapper.add(node);
      mapper.add(node2);
      mapper.add(childNode);
      node.addChild(childNode);

      // get data
      Node storedNode = mapper.get(":testNode1");
      Node storedChildNode = mapper.get(":testNode1:testNode1a");
      Node storedNode2 = mapper.get(":testNode2");
      assertFalse("Node :testNode1 may not be a skeleton if fetched", storedNode.isSkeleton());
      assertFalse("Node :testNode1:testNode1a may not be a skeleton if fetched",
          storedChildNode.isSkeleton());
      assertFalse("Node :testNode2 may not be a skeleton if fetched", storedNode2.isSkeleton());

      // compare data
      assertEquals("comparing parent node", node, storedNode);
      assertEquals("comparing child node", childNode, storedChildNode);
      assertEquals("comparing parent node2", node2, storedNode2);
      assertEquals("checking for child node count", 1, storedNode.getChildren().size());
    }
  }

  @Test
  public void testLocalePersistenceOnNode() throws StorageException {
    for (StorageMapper mapper : mapperList) {
      System.out.println("## Testing mapper " + mapper + " in " + (new Object() {
      }).getClass().getEnclosingMethod().getName());

      NodeValue nv = new NodeValueImpl("key", "value", "type", "description", 1);
      nv.setDescription("Deutsche Beschreibung", Locale.GERMAN);
      nv.setDescription("Schwiizertüütschi Beschriibig", new Locale("de", "ch"));
      nv.setValue("Wert", Locale.GERMAN);
      nv.setValue("Au ä Wert", new Locale("de", "ch"));
      NodeImpl node = new NodeImpl(":testNode1");

      node.addValue(nv);
      System.out.println("## Stored " + node);

      // write data
      NodeImpl node2 = new NodeImpl("testNode2", "");
      mapper.add(node);
      mapper.add(node2);
      NodeImpl childNode = new NodeImpl(":testNode1:testNode1a");
      mapper.add(childNode);

      node.addChild(childNode);

      // get data
      Node storedNode = mapper.get(":testNode1");
      Node storedChildNode = mapper.get(":testNode1:testNode1a");
      Node storedNode2 = mapper.get(":testNode2");
      assertFalse("Node :testNode1 may not be a skeleton if fetched", storedNode.isSkeleton());
      assertFalse("Node :testNode1:testNode1a may not be a skeleton if fetched",
          storedChildNode.isSkeleton());
      assertFalse("Node :testNode2 may not be a skeleton if fetched", storedNode2.isSkeleton());
      System.out.println("## got    " + storedNode);

      // compare data
      assertEquals("comparing parent node", node, storedNode);
      assertEquals("comparing child node", childNode, storedChildNode);
      assertEquals("comparing parent node2", node2, storedNode2);
      assertEquals("checking for child node count", 1, storedNode.getChildren().size());

      node.removeChild(childNode.getName());
      mapper.delete(childNode.getPath());

      mapper.delete(node.getPath());

      mapper.delete(node2.getPath());
    }
  }

  @Test
  public void testTombstones() throws StorageException {
    // TODO activate once dummyMapper is fixed
    for (StorageMapper mapper : mapperList) {
      System.out.println("## Testing mapper " + mapper + " in " + (new Object() {
      }).getClass().getEnclosingMethod().getName());
      // add a Node
      //StorageMapper mapper = new H2SqlMapper("jdbc:h2:./testdb;AUTO_SERVER=TRUE", "sa2", "1234");
      mapper.add(new NodeImpl(":parent"));
      NodeImpl node = new NodeImpl("testNode1", ":parent", Visibility.GREEN);
      node.setOwner("theOwner");
      mapper.add(node);

      // delete the Node and check return
      Node deletedNode = mapper.delete(node.getPath());
      assertEquals("checking deleted Node", node, deletedNode);

      // get tombstone from mapper
      Node tombstoneNode = mapper.get(node.getPath());
      assertTrue("check if node is a tombstone", tombstoneNode.isTombstone());
      assertEquals("check path", node.getPath(), tombstoneNode.getPath());
      assertEquals("check values", 0, tombstoneNode.getValues().size());
      assertNull("check owner", tombstoneNode.getOwner());
      assertEquals("check children", 0, tombstoneNode.getChildren().size());
      assertEquals("check visibility", node.getVisibility(), tombstoneNode.getVisibility());

      // test add Tombstone

      Node testAddNode = new NodeImpl(":testAddNodePath", true);
      assertEquals("check constructor visibility", Visibility.RED,
          testAddNode.getVisibility());
      mapper.add(testAddNode);
      Node returnedNode = mapper.get(":testAddNodePath");
      assertTrue("check if node is a tombstone", returnedNode.isTombstone());
      assertEquals("check path", testAddNode.getPath(), returnedNode.getPath());
      assertEquals("check values", 0, returnedNode.getValues().size());
      assertNull("check owner", returnedNode.getOwner());
      assertEquals("check children", 0, returnedNode.getChildren().size());
      assertEquals("check visibility", testAddNode.getVisibility(),
          returnedNode.getVisibility());
    }
  }

  @Test
  public void testAddSkeleton() throws StorageException {
    for (StorageMapper mapper : mapperList) {
      System.out.println("## Testing mapper " + mapper + " in " + (new Object() {
      }).getClass().getEnclosingMethod().getName());
      // try to add a skeleton node
      assertThrows(StorageException.class,
          () -> mapper.add(new NodeImpl(":TestNode",
              new GenericController("theOwner", new DummyMapper()))));
    }
  }

  @Test
  public void testGetChildren() throws StorageException {
    for (StorageMapper mapper : mapperList) {
      System.out.println("## Testing mapper " + mapper + " in " + (new Object() {
      }).getClass().getEnclosingMethod().getName());
      // create Node with child and subchild
      Node parent = new NodeImpl(":parent");
      Node child = new NodeImpl("child", ":parent");
      Node subChild0 = new NodeImpl("subChild", ":parent:child");
      child.addChild(subChild0);
      parent.addChild(child);
      // add nodes
      mapper.add(parent);
      mapper.add(child);
      mapper.add(subChild0);

      Node storedParent = mapper.get(":parent");
      assertEquals("checking child reference", parent.getChildNodesCsv(),
          storedParent.getChildNodesCsv());
      //assertEquals("checking stored child", child,
      //    parent.getChildren().values().toArray()[0]);

      Node storedChild = mapper.get(":parent:child");
      assertEquals("checking subChild reference", child.getChildNodesCsv(),
          storedChild.getChildNodesCsv());
      //assertEquals("checking stored subChild", subChild0,
      //   storedChild.getChildren().values().toArray()[0]);

      Node storedSubChild = mapper.get(":parent:child:subChild");
      assertEquals("checking child references", "",
          storedSubChild.getChildNodesCsv());
      assertEquals("checking subChild", subChild0, storedSubChild);

      // add more subChilds
      Node subChild1 = new NodeImpl("subChild1", ":parent:child");
      Node subChild2 = new NodeImpl("subChild2", ":parent:child");
      Node subChild3 = new NodeImpl("subChild3", ":parent:child");
      child.addChild(subChild1);
      child.addChild(subChild2);
      child.addChild(subChild3);
      mapper.update(child);
      storedChild = mapper.get(":parent:child");
      assertEquals("checking subChild reference", child.getChildNodesCsv(),
          storedChild.getChildNodesCsv());
      /*
      int counter = 0;
      for (Node n : child.getChildren().values()) {
        assertEquals("checking stored subChild", n,
            storedChild.getChildren().values().toArray()[counter]);
        ++counter;
      }*/

      // update the parent
      storedParent = mapper.get(":parent");
      NodeValue value = new NodeValueImpl("someKey", "someValue");
      storedParent.addValue(value);
      mapper.update(storedParent);
      storedParent = mapper.get(":parent");
      // check references
      assertEquals("checking parent references", parent.getChildNodesCsv(),
          storedParent.getChildNodesCsv());
      storedChild = mapper.get(":parent:child");
      assertEquals("checking child references", child.getChildNodesCsv(),
          storedChild.getChildNodesCsv());

    }
  }
}
