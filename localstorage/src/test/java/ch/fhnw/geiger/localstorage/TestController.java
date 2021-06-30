package ch.fhnw.geiger.localstorage;

import static ch.fhnw.geiger.localstorage.Visibility.RED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import ch.fhnw.geiger.localstorage.db.GenericController;
import ch.fhnw.geiger.localstorage.db.data.Node;
import ch.fhnw.geiger.localstorage.db.data.NodeImpl;
import ch.fhnw.geiger.localstorage.db.data.NodeValue;
import ch.fhnw.geiger.localstorage.db.data.NodeValueImpl;
import ch.fhnw.geiger.localstorage.db.mapper.DummyMapper;
import ch.fhnw.geiger.localstorage.db.mapper.H2SqlMapper;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/***
 * <p>Test the controller capabilities.</p>
 */
public class TestController {

  StorageController controller;

  /**
   * Set up the controller for testing.
   *
   * @throws StorageException if controller could not be set up
   */
  @Before
  public void setup() throws StorageException {
    // TODO add support for multiple mappers to ensure the same functionality with all mappers
    controller = new GenericController("testOwner", new DummyMapper());
    controller.zap();
  }

  @Test
  public void testOwnerUpdateOnNode() throws StorageException {
    System.out.println("## Testing controller in " + (new Object() {
    }).getClass().getEnclosingMethod().getName());
    Node node = new NodeImpl("testNode1", "");
    controller.add(node);
    node = controller.get(node.getPath());
    assertEquals("testing that owner is set upon adding", "testOwner", node.getOwner());
  }

  @Test
  public void testStorageNodeCreate() throws StorageException {
    controller.add(new NodeImpl("testNode1", ""));

    // fetch stored node
    Node storedNode = controller.get(":testNode1");

    // check results
    assertEquals("testOwner", storedNode.getOwner());
    assertEquals("testNode1", storedNode.getName());
    assertEquals(":testNode1", storedNode.getPath());
    assertSame(RED, storedNode.getVisibility());
  }

  @Test
  public void testStorageNodeAdd() throws StorageException {
    controller.add(new NodeImpl("parent1", ""));
    controller.add(new NodeImpl("name2", ":parent1"));

    // get the record
    Node storedNode = controller.get(":parent1:name2");

    // check results
    assertEquals("testOwner", storedNode.getOwner());
    assertEquals("name2", storedNode.getName());
    assertEquals(":parent1:name2", storedNode.getPath());
    assertSame(RED, storedNode.getVisibility());

  }

  /**
   * depends on correct functionality of the StorageController.create() function
   */
  @Test
  public void testStorageNodeUpdate() throws StorageException {
    // create original node
    Node parent = new NodeImpl("parent1", "");
    controller.add(parent);

    // updated Node with different visibility children
    NodeImpl node = new NodeImpl("testNode1", ":parent1", Visibility.GREEN);
    controller.add(node);
    assertEquals("testOwner", node.getOwner());
    NodeImpl sn = new NodeImpl("testChild1", ":parent1:testNode1");
    controller.add(sn);
    node.setVisibility(Visibility.RED);
    node.addChild(sn);

    // update with node from above
    controller.update(node);

    // get the record
    Node storedNode = controller.get(":parent1:testNode1");

    // check results
    assertEquals("testOwner", storedNode.getOwner());
    assertEquals("testNode1", storedNode.getName());
    assertEquals(":parent1:testNode1", storedNode.getPath());
    assertEquals("testChild1", storedNode.getChildNodesCsv());
    assertSame(RED, storedNode.getVisibility());

    // update parent
    parent.addValue(new NodeValueImpl("somekey", "someValue"));
    controller.update(parent);

    // check children
    Node storedChild = controller.get(":parent1:testNode1");
    String[] children = storedChild.getChildNodesCsv().split(",");
    assertEquals("check number of children", 1, children.length);
    assertEquals("checking childnode", sn, storedChild.getChildren().values().toArray()[0]);

  }

  @Test
  public void testValueUpdate() throws StorageException {
    try {
      controller.add(new NodeImpl("testNode1", ":parent1"));
      fail("Should raise an exception as parent node does not exist");
    } catch (StorageException e) {
      // this is the expected behavior as the parent node does not exist
    }
    NodeImpl[] n = new NodeImpl[]{
        new NodeImpl("parent1"),
        new NodeImpl("testNode1", ":parent1")};
    for (NodeImpl tn : n) {
      System.out.println("## creating new node " + tn.getPath() + " (parent of "
          + tn.getParentPath() + ")");
      controller.add(tn);
    }

    // add a value
    System.out.println("## adding value");
    controller.addValue(":parent1:testNode1", new NodeValueImpl("key1", "valueFirst"));

    // update value
    System.out.println("## updating value");
    NodeValue value2 = new NodeValueImpl("key1", "valueSecond");
    controller.updateValue(":parent1:testNode1", value2);

    // get the record
    System.out.println("## testing updated value");
    Node n2 = controller.get(":parent1:testNode1");
    assertEquals("stored values do not match", value2.getValue(),
        n2.getValue(value2.getKey()).getValue());

    try {
      System.out.println("## testing removal of node with child nodes ("
          + n[n.length - 1].getPath() + ")");
      controller.delete(n[0].getPath());
      fail("should raise an exception as child classes exist");
    } catch (StorageException e) {
      // this is expected as the node has sub-nodes
    }

    List<NodeImpl> tearDown = Arrays.asList(n);
    Collections.reverse(tearDown);
    for (NodeImpl tn : tearDown) {
      System.out.println("## removing node \"" + tn.getPath() + "\" (parent of "
          + tn.getParentPath() + ")");
      controller.delete(tn.getPath());
    }
  }

  @Test
  public void testStorageNodeRemove() throws StorageException {
    controller.add(new NodeImpl("parent1", ""));
    NodeImpl node = new NodeImpl("name1", ":parent1");
    NodeValue nv = new NodeValueImpl("key", "value");
    node.addValue(nv);
    controller.add(node);
    Node removed = controller.delete(":parent1:name1");

    // check nodes
    assertEquals(node, removed);
    assertThrows(StorageException.class, () -> controller.get(removed.getPath()));

    // check values
    assertEquals(nv, removed.getValue("key"));
    assertThrows(StorageException.class, () -> controller.getValue(removed.getPath(), "key"));
  }

  @Test
  public void testStorageNodeRemoveWithChild() throws StorageException {
    controller.addOrUpdate(new NodeImpl("parent1", ""));
    NodeImpl node = new NodeImpl("name1", ":parent1");
    // add child
    NodeImpl childNode = new NodeImpl("child1", ":parent1:name1");
    node.addChild(childNode);
    controller.addOrUpdate(node);
    controller.addOrUpdate(childNode);

    assertThrows(StorageException.class, () -> controller.delete(":parent1:name1"));

    // check if node still exists
    assertEquals(node, controller.get(":parent1:name1"));
  }

  @Test
  public void testStorageNodeSearch() throws StorageException {

    controller.add(
        new NodeImpl(":parent1", null,
            new NodeValue[]{
                new NodeValueImpl("parentKey", "parentValue")
              },
            new Node[]{
                new NodeImpl(":parent1:child0", null,
                    new NodeValueImpl[0],
                    new Node[] {
                        new NodeImpl(":parent1:child0:empty")
                      }
                ),
                new NodeImpl(":parent1:child1", null,
                    new NodeValue[] {
                        new NodeValueImpl("name1", "value1")
                      },
                    new Node[0]
                )
              }
        )
    );

    SearchCriteria criteria = new SearchCriteria(":parent1", "name1", null);
    testcaseSearchCriteria("searching single child node",
        controller, criteria, new String[] {":parent1:child1"});

    criteria = new SearchCriteria(":parent1:child0", "name1", null);
    testcaseSearchCriteria("searching single child node in a sub branch "
        + "without matching node", controller, criteria, new String[] {});
  }

  private static void testcaseSearchCriteria(String caseName, StorageController controller,
                                             SearchCriteria criteria, String[] paths)
      throws StorageException {
    List<Node> result = controller.search(criteria);
    List<String> l = Arrays.asList(paths);
    Assert.assertEquals("(" + caseName + ") Bad number of results returned",
        paths.length, result.size());
    for (Node n : result) {
      Assert.assertTrue("(" + caseName + ") Unable to find expected Node ",
          l.contains(n.getPath()));
    }
  }

  @Test
  @Ignore
  public void testRegisterChangeListener() {
    fail("not implemented");
  }

  @Test
  @Ignore
  public void testNotifyChangeListener() {
    fail("not implemented");
  }

  @Test
  public void testRenameNode() throws StorageException {
    NodeImpl[] nodes = new NodeImpl[]{
        new NodeImpl("renameTests"),
        new NodeImpl("name1", ":renameTests"),
        new NodeImpl("name11", ":renameTests:name1"),
        new NodeImpl("name2", ":renameTests"),
        new NodeImpl("name21", ":renameTests:name2"),
        new NodeImpl("name3", ":renameTests")};
    for (NodeImpl n : nodes) {
      controller.add(n);
    }
    // rename by name
    controller.rename(":renameTests:name1", "name1a");
    // rename by path
    controller.rename(":renameTests:name2", ":renameTests:name2a");

    // check old nodes
    assertThrows(StorageException.class, () -> controller.get(":renameTests:name1"));
    assertThrows(StorageException.class, () -> controller.get(":renameTests:name2"));

    // check new nodes
    assertNotNull("renaming node seems unsuccessful (new node missing)",
        controller.get(":renameTests:name1a"));
    assertNotNull("renaming node seems unsuccessful (new node missing)",
        controller.get(":renameTests:name2a"));

    // check name
    assertEquals("renaming node seems unsuccessful (new node name wrong)",
        "name1a", controller.get(":renameTests:name1a").getName());
    assertEquals("renaming node seems unsuccessful (new node name wrong)",
        "name2a", controller.get(":renameTests:name2a").getName());

    // check path
    assertEquals("renaming node seems unsuccessful (new node path wrong)",
        ":renameTests:name1a", controller.get(":renameTests:name1a").getPath());
    assertEquals("renaming node seems unsuccessful (new node path wrong)",
        ":renameTests:name2a", controller.get(":renameTests:name2a").getPath());

    // check child nodes
    assertNotNull("renaming node seems unsuccessful (sub-node missing)",
        controller.get(":renameTests:name1a:name11"));
    assertNotNull("renaming node seems unsuccessful (sub-node missing)",
        controller.get(":renameTests:name2a:name21"));

    // check child node name
    assertEquals("renaming node seems unsuccessful (sub-node name wrong)",
        "name11", controller.get(":renameTests:name1a:name11").getName());
    assertEquals("renaming node seems unsuccessful (sub-node name wrong)",
        "name21", controller.get(":renameTests:name2a:name21").getName());

    // check child node path
    assertEquals("renaming node seems unsuccessful (sub-node path wrong)",
        ":renameTests:name1a:name11", controller.get(":renameTests:name1a:name11").getPath());
    assertEquals("renaming node seems unsuccessful (sub-node path wrong)",
        ":renameTests:name2a:name21", controller.get(":renameTests:name2a:name21").getPath());

    // test rename of non existing nodes
    assertThrows(StorageException.class,
        () -> controller.rename(":renameTests:name4", ":renameTests:name4a"));
    assertThrows(StorageException.class,
        () -> controller.rename(":renameTests:name4", "name4a"));

    // test rename to an existing node
    assertThrows(StorageException.class,
        () -> controller.rename(":renameTests:name2a", ":renameTests:name3"));
    assertThrows(StorageException.class,
        () -> controller.rename(":renameTests:name2a", "name3"));
  }

  @Test
  public void testRenameNodeWithValues() throws StorageException {
    NodeImpl[] nodes = new NodeImpl[]{
        new NodeImpl("renameTests"),
        new NodeImpl("name1", ":renameTests"),
        new NodeImpl("name2", ":renameTests"),
        new NodeImpl("name21", ":renameTests:name2"),
        new NodeImpl("name3", ":renameTests")
      };

    NodeValue nv = new NodeValueImpl("key", "value");
    NodeValue nv1 = new NodeValueImpl("key1", "value1");
    NodeValue nv2 = new NodeValueImpl("key2", "value2");
    NodeValue nv21 = new NodeValueImpl("key21", "value21");

    nodes[0].addValue(nv);
    nodes[1].addValue(nv1);
    nodes[2].addValue(nv2);
    nodes[3].addValue(nv21);

    for (NodeImpl n : nodes) {
      controller.add(n);
    }
    controller.rename(":renameTests:name2", ":renameTests:name2a");

    // check old node
    assertThrows(StorageException.class, () -> controller.get(":renameTests:name2"));

    assertNotNull("renaming node seems unsuccessful (new node missing)",
        controller.get(":renameTests:name2a"));
    assertEquals("renaming node seems unsuccessful (new node name wrong)",
        "name2a", controller.get(":renameTests:name2a").getName());
    assertEquals("renaming node seems unsuccessful (new node path wrong)",
        ":renameTests:name2a", controller.get(":renameTests:name2a").getPath());
    assertNotNull("renaming node seems unsuccessful (sub-node missing)",
        controller.get(":renameTests:name2a:name21"));
    assertEquals("renaming node seems unsuccessful (sub-node name wrong)",
        "name21", controller.get(":renameTests:name2a:name21").getName());
    assertEquals("renaming node seems unsuccessful (sub-node path wrong)",
        ":renameTests:name2a:name21", controller.get(":renameTests:name2a:name21").getPath());

    // check values
    assertEquals("value lost on parent", nv,
        controller.get(":renameTests").getValue("key"));
    assertEquals("value lost on sibling", nv1,
        controller.get(":renameTests:name1").getValue("key1"));
    assertEquals("value lost moved node", nv2,
        controller.get(":renameTests:name2a").getValue("key2"));
    assertEquals("value lost on sub-node", nv21,
        controller.get(":renameTests:name2a:name21").getValue("key21"));

    // check old values
    assertThrows(StorageException.class,
        () -> controller.getValue(":renameTests:name2", "key2"));
    assertThrows(StorageException.class,
        () -> controller.getValue(":renameTests:name2:name21", "key21"));
  }

  @Test
  public void testSkeletonMaterialization() throws StorageException {
    StorageController controller = new GenericController("owner",
        new H2SqlMapper("jdbc:h2:./testdb;AUTO_SERVER=TRUE",
            "sa2", "1234"));
    controller.zap();
    // Create empty node :Global:threats
    Node threats = new NodeImpl(":GlobalN", null, new NodeValue[]{
        new NodeValueImpl("name", "parent node")}, null);
    controller.add(threats);

    // create hashmap of values to be added
    Map<String, String[]> threatMap = new HashMap<>();
    threatMap.put("80efffaf-98a1-4e0a-8f5e-gr89388350ma", new String[]{"Malware"});
    threatMap.put("80efffaf-98a1-4e0a-8f5e-gr89388351wb", new String[]{"Web-based threats"});

    // populate :Global:threats with values according to hashmap
    for (Map.Entry<String, String[]> e : threatMap.entrySet()) {
      Node threat = new NodeImpl(":GlobalN:" + e.getKey(), Visibility.RED,
          new NodeValue[]{
              new NodeValueImpl("name", e.getValue()[0]),
              new NodeValueImpl("GEIGER_threat", e.getValue().length > 1
                  ? e.getValue()[1] : e.getValue()[0])
            }, null
      );
      controller.add(threat);
    }
    System.out.println();
    threats = controller.get(":GlobalN");

    Assert.assertFalse("Checking that main is not skeletonized",
        threats.isSkeleton());
    Assert.assertEquals("Checking that child nodes are included", 2,
        threats.getChildren().size());
    Assert.assertEquals("Checking that child nodes are included in CVS", 2,
        threats.getChildNodesCsv().split(",").length);
  }

  @Test
  public void addOrUpdateTest() throws StorageException {
    Node n = new NodeImpl(":TestNode", null,
        new NodeValue[]{
            new NodeValueImpl("key1", "value1")
          },
        new Node[]{
            new NodeImpl(":TestNode:node1"),
            new NodeImpl(":TestNode:node2"),
            new NodeImpl(":TestNode:node3"),
          }
    );
    Assert.assertTrue("Failed writing first time (wrong return value)",
        controller.addOrUpdate(n));
    Assert.assertEquals("Failed writing first time. "
        + "Failed to compare written to provided node", n, controller.get(n.getPath()));
    for (int i = 1; i < 3; i++) {
      Assert.assertEquals("Failed writing first time. "
          + "Failed to compare written to provided node", ":TestNode:node"
          + i, controller.get(":TestNode:node" + i).getPath());
    }

    assertFalse("Adding a skeleton node",
        controller.addOrUpdate(new NodeImpl(":TestNode:node4", controller)));

    assertThrows(StorageException.class, () -> controller.get(":TestNode:node4"));
    assertFalse("Failed writing first time (wrong return value)", controller.addOrUpdate(n));
  }

  @Test
  public void testOmittingChildrenOnUpdate() throws StorageException {
    // testing if children are omitted when parent node is updated with skeleton childnodes
    // this test is a result of a bug found in GeigerIndicator

    // building the necessary nodes
    Node plugin = new NodeImpl(":Local:plugin");
    controller.add(plugin);
    // add plugin children
    Node pluginChild0 = new NodeImpl(":Local:plugin:child0");
    Node pluginChild1 = new NodeImpl(":Local:plugin:child1");
    controller.add(pluginChild0);
    controller.add(pluginChild1);

    // this section did omit the children
    Node localNode = controller.get(":Local");
    NodeValue userId = new NodeValueImpl("currentUser", "328161f6-89bd-49f6-user-5a375ff56ana");
    localNode.updateValue(userId);
    NodeValue deviceId = new NodeValueImpl("currentDevice", "328161f6-89bd-49f6-dev1-5a375ff56ana");
    localNode.updateValue(deviceId);
    controller.update(localNode);


    // this section threw the error
    plugin = controller.get(":Local:plugin");
    String[] children = plugin.getChildNodesCsv().split(",");
    assertEquals("checking children", 2, plugin.getChildren().size());
  }
}
