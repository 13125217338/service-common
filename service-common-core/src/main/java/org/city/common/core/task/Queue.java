package org.city.common.core.task;

/**
 * @作者 ChengShi
 * @日期 2020-04-23 21:27:06
 * @版本 1.0
 * @描述 单链表队列，先进后出（线程安全）
 */
final class Queue<T>{
	Queue() {}
	private Data head = null;
	private int size = 0;
	
	/**
	 * @描述 获取当前大小
	 * @return 当前大小
	 */
	int size(){return size;}
	
	/**
	 * @描述 增加队列（往头添加，size加1）
	 * @param value 值
	 */
	synchronized void add(T value){
		Data data = new Data();
		data.value = value;
		data.next = head;
		head = data;
		size++;
	}
	
	/**
	 * @描述 获取头信息并清理头信息（size减1）
	 * @return 头信息
	 */
	synchronized T getHead(){
		if (head == null) {return null;}
		T value = head.value;
		Data next = head.next;
		head.next = null;
		head = next;
		size--;
		return value;
	}
	
	/**
	 * @作者 ChengShi
	 * @日期 2020-04-23 21:26:49
	 * @版本 1.0
	 * @parentClass Queue
	 * @描述 队列链表
	 */
	private class Data{
		private T value;
		private Data next;
	}
	
	/**
	 * @描述 清除数据（赋NULL）
	 */
	synchronized void removeAll(){head = null;size = 0;}
}
