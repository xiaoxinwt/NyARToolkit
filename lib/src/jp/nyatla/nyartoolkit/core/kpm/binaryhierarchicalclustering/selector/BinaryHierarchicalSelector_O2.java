package jp.nyatla.nyartoolkit.core.kpm.binaryhierarchicalclustering.selector;




import jp.nyatla.nyartoolkit.core.kpm.binaryhierarchicalclustering.BinaryHierarchicalNode;
import jp.nyatla.nyartoolkit.core.kpm.binaryhierarchicalclustering.selector.utils.LimitedPriorityNodeList;
import jp.nyatla.nyartoolkit.core.kpm.binaryhierarchicalclustering.selector.utils.NodeStack;
import jp.nyatla.nyartoolkit.core.kpm.keyframe.FreakMatchPointSetStack;
import jp.nyatla.nyartoolkit.core.kpm.utils.LongDescripter768;

/**
 * 最適化した{@link BinaryHierarchicalSelector}
 * 実行時newの削除と、PriorityQueueの代替実装クラスへの置き換え
 */
public class BinaryHierarchicalSelector_O2
{


	final public FreakMatchPointSetStack.Item[] _result;
	final private LimitedPriorityNodeList mlist;
	//特徴量の一時バッファ
	final private NodeStack _node_stack=new NodeStack(1000);
	
	private int _num_of_result;
	public BinaryHierarchicalSelector_O2(int i_MaxNodesToPop,int i_max_result)
	{
		this._result=new FreakMatchPointSetStack.Item[i_max_result];
		this.mlist=new LimitedPriorityNodeList(i_MaxNodesToPop);
		return;
	}
	/**
	 * Query the tree for a reverse index.
	 */
	public int query(BinaryHierarchicalNode i_node,LongDescripter768 feature)
	{
		this._num_of_result=0;
		this.mlist.reset();
		NodeStack nodes = this._node_stack;
		nodes.clear();
		if(i_node.is_leaf){
			this.append(i_node);
		}else{
			this.query(this.mlist,nodes, i_node, feature);
		}
		return (int) this._num_of_result;
	}



	private void append(BinaryHierarchicalNode i_node)
	{
		assert i_node.is_leaf==true;
		//末端なら結果配列へ値を追加
		int p=this._num_of_result;
		int l=i_node.reserv_index.length;
		//
		if(l+p>this._result.length){
			l=this._result.length-this._num_of_result;
		}
		for(int i=0;i<l;i++){
			this._result[p+i]=i_node.reserv_index[i];
		}
		this._num_of_result+=l;
		return;
	}
	/**
	 * Recursive function query function.
	 */
	private void query(LimitedPriorityNodeList queue,NodeStack i_nodes,BinaryHierarchicalNode node, LongDescripter768 feature)
	{
		assert (!node.is_leaf);

		//近傍ノードをスタックに追加
		int sp=i_nodes.getLength();
		int num_of_min=nearest(node,i_nodes, queue, feature);
		
		//先頭からnum_of_min個の最小ノードについて再帰探索
		for (int i = 0; i < num_of_min; i++) {
			BinaryHierarchicalNode n=i_nodes.getItem(sp+i).node;
			if(n.is_leaf){
				this.append(n);
			}else{
				this.query(queue,i_nodes,n,feature);
			}
		}
		//好成績な近傍ノード1つを取り出して探索する。
		NodeStack.Item item=this.mlist.popSmallest();
		if (item!=null) {
			BinaryHierarchicalNode n = item.node;
			if(n.is_leaf){
				this.append(n);
			}else{
				this.query(queue,i_nodes,n,feature);
			}
		}
		return;
	}

    /**
     * Get a queue of all the children nodes sorted by distance from node center.
     */
    private static int nearest(BinaryHierarchicalNode i_node,NodeStack nodes,LimitedPriorityNodeList queue,LongDescripter768 feature)
    {
        int mind = Integer.MAX_VALUE;

        int sp=nodes.getLength();
        BinaryHierarchicalNode[] children=i_node.children;
        int num_of_children=children.length;
        
        //最小値の探索
        for(int i = 0; i < num_of_children; i++) {      	
        	NodeStack.Item item=nodes.prePush();
        	if(item==null){
        		//ワークエリアを使い切った。
        		return 0;
        	}
            int d = children[i].center.descripter.hammingDistance(feature);
            item.node=children[i];
            item.distance=d;
            if(d < mind) {
                mind = d;
            }
        }
        int num_of_min=0;
        //最小値以外をキューに追記
        for(int i=0;i<num_of_children;i++){
        	NodeStack.Item item=nodes.getItem(sp+i);
        	if(item.distance==mind){
        		//最小値は先頭に移動
        		nodes.swap(sp+num_of_min,sp+i);
        		num_of_min++;
        	}else{
        		//最小値以外はキューに追加
        		queue.push(item);
        		
        	}
        }        
        //最小値の数は(min_add_idx-s_idx)
        return num_of_min;
    }
	


}
