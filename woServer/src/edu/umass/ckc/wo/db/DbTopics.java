package edu.umass.ckc.wo.db;

import edu.umass.ckc.wo.beans.Topic;
import edu.umass.ckc.wo.cache.ProblemMgr;
import edu.umass.ckc.wo.content.CCStandard;
import edu.umass.ckc.wo.content.Problem;
import edu.umass.ckc.wo.content.TopicIntro;
import edu.umass.ckc.wo.tutor.Settings;

import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.Set;

/**
 * <p> Created by IntelliJ IDEA.
 * User: david
 * Date: Jul 17, 2008
 * Time: 10:31:46 AM
 */
public class DbTopics {




    /**
     *  Get the Topics in the default class lesson plan.   The default class lesson plan is the set of rows in classlessonplan
     * that is marked isDefault=1.
     * @param conn
     * @return A List of Topic objects in the order they will be presented
     * @throws SQLException
     */
    public static List<Topic> getDefaultTopicsSequence(Connection conn) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String q = "select probGroupId, topic.description, seqPos from classlessonplan, problemgroup topic where isDefault=1 and probGroupId=topic.id order by seqPos";
            ps = conn.prepareStatement(q);
            rs = ps.executeQuery();
            List<Topic> topics = new ArrayList<Topic>();
            while (rs.next()) {
                Topic t = new Topic(rs.getInt(1),rs.getString(2));
                t.setSeqPos(rs.getInt(3));
                t.setOldSeqPos(t.getSeqPos());
                topics.add(t);
            }
            return topics;
        } finally {
            if (rs != null)
                rs.close();
            if (ps != null)
                ps.close();

        }
    }

    public static List<Topic> getAllTopics (Connection conn) throws SQLException {
        ResultSet rs=null;
        PreparedStatement stmt=null;
        List<Topic> topics = new ArrayList<Topic>();
        try {
            String q = "select id, description from problemgroup where active=1 order by id";
            stmt = conn.prepareStatement(q);
            rs = stmt.executeQuery();
            while (rs.next()) {
                Topic t = new Topic(rs.getInt(1),rs.getString(2));
                topics.add(t);
            }
            return topics;
        }
        finally {
            if (stmt != null)
                stmt.close();
            if (rs != null)
                rs.close();
        }
    }

 public static List<Topic> getClassInactiveTopics(Connection conn, int classId, List<Topic> activeTopics) throws SQLException {
        ResultSet rs=null;
        PreparedStatement stmt=null;
        try {
            StringBuilder sb = new StringBuilder();
            for (Topic t: activeTopics)
                sb.append(t.getId() + ",");
            String ids = sb.substring(0,sb.toString().length()-1);
            String q = "select id, description from problemgroup where active=1 and " +
                     "id not in (" + ids + ")";

            stmt = conn.prepareStatement(q);
            rs = stmt.executeQuery();
            List<Topic> topics = new ArrayList<Topic>();

            while (rs.next()) {
                Topic t = new Topic(rs.getInt(1),rs.getString(2));
                topics.add(t);
            }
            return topics;
        }
        finally {
            if (stmt != null)
                stmt.close();
            if (rs != null)
                rs.close();
        }
 }

   

    public static List<Topic> getClassActiveTopics (Connection conn, int classId) throws SQLException {
        List<Topic> topics = getClassActiveTopics2(conn,classId,false);
        if (topics.size() == 0)
            return getClassActiveTopics2(conn,classId,true);
        else return topics;
    }

    /**
     *
     * @param conn
     * @param classId  Will be used to get active topics for a class when isDefault is false
     * @param isDefault controls whether topics are found for the class or by using the default set of topics
     * @return
     * @throws SQLException
     */
    public static List<Topic> getClassActiveTopics2 (Connection conn, int classId, boolean isDefault) throws SQLException {
        ResultSet rs=null;
        PreparedStatement stmt=null;
        try {
            String q = "select probGroupId, topic.description, seqPos from classlessonplan, problemgroup topic where " +
                    (isDefault ? "isDefault=1" : "classid=?") + " and topic.id=probGroupId and seqPos > 0 and topic.active=1 and topic.id in (select distinct pgroupid from probprobgroup) order by seqPos";
            stmt = conn.prepareStatement(q);
            if (!isDefault)
                stmt.setInt(1,classId);
            rs = stmt.executeQuery();
            List<Topic> topics = new ArrayList<Topic>();
            while (rs.next()) {
                Topic t = new Topic(rs.getInt(1),rs.getString(2));
                t.setSeqPos(rs.getInt(3));
                t.setOldSeqPos(t.getSeqPos());
                // We get the set of CCStandards for this topic from the ProblemMgr
                Set<CCStandard> stds = ProblemMgr.getTopicStandards(t.getId());
                t.setCcStandards(stds);
                topics.add(t);
            }
            return topics;
        }
        finally {
            if (stmt != null)
                stmt.close();
            if (rs != null)
                rs.close();
        }

    }

    /**
     * Given the id of a class, clone its lesson plan for another class.
     * @param conn
     * @param classId
     * @param newClassId
     * @throws SQLException
     */
    public static void cloneLessonPlan (Connection conn, int classId, int newClassId) throws SQLException {
        ResultSet rs=null;
        PreparedStatement stmt=null;
        try {
            String q = "select seqPos,probGroupId from classlessonplan where classId=?";
            stmt = conn.prepareStatement(q);
            stmt.setInt(1,classId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                int pos= rs.getInt(1);
                int topicId = rs.getInt(2);
                insertTopic(conn, newClassId, pos, topicId);
            }
        }
        finally {
            if (stmt != null)
                stmt.close();
            if (rs != null)
                rs.close();
        }
    }

    private static void insertTopic(Connection conn, int newClassId, int pos, int topicId) throws SQLException {
        PreparedStatement stmt=null;
        try {
            String q = "insert into classlessonplan (classId,seqPos,probGroupId) values (?,?,?)";
            stmt = conn.prepareStatement(q);
            stmt.setInt(1,newClassId);
            stmt.setInt(2, pos);
            stmt.setInt(3, topicId);
            stmt.execute();
        }

        finally {

            if (stmt != null)
                stmt.close();
        }
    }

    private static void addTopicCCStandards (Connection conn, List<Topic> topics)  {
        for (Topic t: topics) {


        }
    }

    /**
     * Get the topic sequence for a given class.
     * N.B.  Topics that have been deactivated have a seqPos of -1.  They are included at the end of the list after
     * the active topics.
     * @param conn
     * @param classId
     * @return  a List of Topic objects in the order they will be presented
     * @throws SQLException
     */
    public static List<Topic> getClassTopicsSequence2 (Connection conn, int classId) throws SQLException {
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                String q = "select probGroupId, topic.description, seqPos from classlessonplan, problemgroup topic where classid=? and topic.id=probGroupId and seqPos > 0 order by seqPos ";
                ps = conn.prepareStatement(q);
                ps.setInt(1,classId);
                rs = ps.executeQuery();
                List<Topic> topics = new ArrayList<Topic>();
                while (rs.next()) {
                    Topic t = new Topic(rs.getInt(1),rs.getString(2));
                    t.setSeqPos(rs.getInt(3));
                    t.setOldSeqPos(t.getSeqPos());
                    topics.add(t);
                }
                ps.close();
                rs.close();
                q = "select probGroupId, topic.description from classlessonplan, problemgroup topic where classid=? and topic.id=probGroupId and seqPos < 0 ";
                ps = conn.prepareStatement(q);
                ps.setInt(1,classId);
                rs = ps.executeQuery();
                while (rs.next()) {
                    Topic t = new Topic(rs.getInt(1),rs.getString(2));
                    t.setSeqPos(-1);
                    t.setOldSeqPos(-1);
                    topics.add(t);
                }
                return topics;
            } finally {
                if (rs != null)
                    rs.close();
                if (ps != null)
                    ps.close();

            }
        }

    /**
     * Return the topic sequence for a class.   Will return the default class lesson plan if a customized
     * plan does not exist for the class.   Topics are returned in the order they will be presented with deactivated
     * topics included at the tail of the list.
     * @param conn
     * @param classId
     * @return  List of Topic objects in presentation order.
     * @throws SQLException
     */
    public static List<Topic> getClassTopicsSequence (Connection conn, int classId) throws SQLException {
        List<Topic> topics = getClassTopicsSequence2(conn,classId);
        if (topics.size() == 0)
            return getDefaultTopicsSequence(conn);
        else return topics;
    }





    public static String getTopicName (Connection conn, int topicId) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String q = "select description from problemgroup where id=?";
            ps = conn.prepareStatement(q);
            ps.setInt(1,topicId);
            rs = ps.executeQuery();
            

            if (rs.next()) {
                return rs.getString(1);
            }
            return null;
        } finally {
            if (rs != null)
                rs.close();
            if (ps != null)
                ps.close();

        }
    }

    public static boolean hasLessonPlan (Connection conn, int classId) throws SQLException {
        ResultSet rs=null;
        PreparedStatement stmt=null;
        try {
            String q = "select * from ClassLessonPlan where classId=?";
            stmt = conn.prepareStatement(q);
            stmt.setInt(1,classId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return true;
            }
            else return false;
        }
        finally {
            if (stmt != null)
                stmt.close();
            if (rs != null)
                rs.close();
        }
    }

    /**
     * Remove all the rows for a class lesson plan
     * @param conn
     * @param classId
     * @throws SQLException
     */
    public static void removeClassActiveTopics (Connection conn, int classId) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String q = "delete from classlessonplan where classId=? and seqPos>0";
            ps = conn.prepareStatement(q);
            ps.setInt(1,classId);
            ps.executeUpdate();
        }  finally {
            if (ps != null)
                ps.close();
        }
    }

    /**
     * Remove all the rows for a class lesson plan
     * @param conn
     * @param classId
     * @throws SQLException
     */
    public static void removeClassTopics (Connection conn, int classId) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String q = "delete from classlessonplan where classId=?";
            ps = conn.prepareStatement(q);
            ps.setInt(1,classId);
            ps.executeUpdate();
        }  finally {
            if (ps != null)
                ps.close();
        }
    }


    /**
     * Given a list of Topics that is sorted according to the order of presentation, insert each topic as a row
     * in the classlessonplan table.  N.B.   removeClassTopics (above) should be called first to make sure
     * the previous topic ordering is removed for the class.
     * @param conn
     * @param classId
     * @param topics
     * @throws SQLException
     */
    public static void insertTopics (Connection conn, int classId, List<Topic> topics) throws SQLException {
      PreparedStatement ps = null;
        try {
            String q = "insert into classlessonplan (classId, seqPos, probGroupId, isDefault) values (?,?,?,?)";
            int i=1;
            for (Topic topic: topics) {
                ps = conn.prepareStatement(q);
                ps.setInt(1,classId);
                ps.setInt(2,i++);   // dm used to be topic.seqPos which was creating gaps in the sequence
                ps.setInt(3,topic.getId());
                ps.setInt(4,0); // not a default
                ps.executeUpdate();
            }
        } finally {
            if (ps != null)
                ps.close();

        }
    }

    /**
     * Creates a lesson plan for a class that is the standard set of topics in the default order.
     * @param conn
     * @param classId
     * @throws SQLException
     */
    public static void insertLessonPlanWithDefaultTopicSequence(Connection conn, int classId) throws SQLException {
        List<Topic> topics = getDefaultTopicsSequence(conn);
        insertTopics(conn,classId,topics);
    }


    /**
     * Returns A topic introduction if there is one.
     *
     * @return
     * @throws java.sql.SQLException
     */
    public static TopicIntro getTopicIntro(Connection conn, int topicID) throws SQLException {
        String q = "select intro,type,description from problemGroup where id = ?";
        PreparedStatement ps = conn.prepareStatement(q);
        ps.setInt(1,topicID);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            String intro = rs.getString(1);
            String type = rs.getString(2);
            String descr = rs.getString(3);
            return new TopicIntro(intro,type, descr);
        }
        return null;
    }

    public static void insertClassInactiveTopic(Connection conn, int classId, Topic t) throws SQLException {
        PreparedStatement stmt=null;
        try {
            String q = "insert into classlessonplan (classid, probgroupid, seqpos, isdefault) values (?,?,?,?)";
            stmt = conn.prepareStatement(q);
            stmt.setInt(1,classId);
            stmt.setInt(2,t.getId());
            stmt.setInt(3,-1);
            stmt.setInt(4,0);
            stmt.execute();
        }

        finally {
            if (stmt != null)
                stmt.close();
        }
    }

    public static void cloneOmittedProblems(Connection conn, int classId, int newClassId) throws SQLException {
        ResultSet rs=null;
        PreparedStatement stmt=null;
        PreparedStatement ps=null;
        try {
            String q = "select probId,topicId from classomittedproblems where classid=?";
            stmt = conn.prepareStatement(q);
            stmt.setInt(1,classId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                int probId= rs.getInt(1);
                int topicId= rs.getInt(2);
                ps = conn.prepareStatement("insert into classomittedproblems (classid,probid,topicid) values (?,?,?)");
                ps.setInt(1,newClassId);
                ps.setInt(2,probId);
                ps.setInt(3,topicId);
                ps.executeUpdate();
                ps.close();
            }
        }
        finally {
            if (stmt != null)
                stmt.close();
            if (rs != null)
                rs.close();
        }
    }


    /**
     * A dummy topic exists so that we can insert its id in places where foreign keys insist on legal topicIDs
     * @param conn
     * @return
     * @throws SQLException
     */
    public static int getDummyTopic(Connection conn) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String q = "select id from problemgroup where type='dummy'";
            ps = conn.prepareStatement(q);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            return -1;
        } finally {
            if (rs != null)
                rs.close();
            if (ps != null)
                ps.close();

        }
    }
}
