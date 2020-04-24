package com.spd.nfcdemo.iso15693;

import android.annotation.SuppressLint;
import android.nfc.tech.NfcV;

import java.io.IOException;

public class NfcVUtil {
	private NfcV mNfcV;
	/**UID数组行式*/
	private byte[] ID;
	private String UID;
	private String DSFID;
	private String AFI;
	/**block的个数*/
	private int blockNumber;
	/**一个block长度*/
	private int oneBlockSize;
	/**信息*/
	private byte[] infoRmation;



	/**
	 * 初始化
	 * @return 
	 * @return 
	 * @throws IOException
	 */
	//@SuppressLint("NewApi")
	public NfcVUtil(NfcV mNfcV) throws IOException {
	this.mNfcV = mNfcV;
	ID = this.mNfcV.getTag().getId();
	byte[] uid = new byte[ID.length];
	int j = 0;
	for(int i = ID.length - 1; i >=0; i-- ){
	uid[j] = ID[i];
	j++;
	}
	this.UID = printHexString(uid);
	getInfoRmation();
	}

	public String getUID() {
	return UID;
	}
	/**
	 * 取得标签信息
	 */
	@SuppressLint("NewApi")
	private byte[] getInfoRmation() throws IOException {
	byte[] cmd = new byte[10];
	cmd[0] = (byte) 0x22; // flag
	cmd[1] = (byte) 0x2B; // command
	System.arraycopy(ID, 0, cmd, 2, ID.length); // UID
	infoRmation = mNfcV.transceive(cmd);
	blockNumber = infoRmation[12];
	oneBlockSize = infoRmation[13];
	AFI = printHexString(new byte[]{infoRmation[11]});
	DSFID = printHexString(new byte[]{infoRmation[10]});
	return infoRmation;
	}

	public String getDSFID() {
	return DSFID;
	}

	public String getAFI() {
	return AFI;
	}
	public int getBlockNumber(){
	return blockNumber + 1;
	}

	public int getOneBlockSize() {
	return oneBlockSize + 1;
	}

	/**
	 * 读取一个位置在position的block
	 * @param position 要读取的block位置
	 * @return 返回内容字符串
	 * @throws IOException
	 */
	public String readOneBlock(int position) throws IOException {
		byte cmd[] = new byte[11];
		cmd[0] = (byte) 0x22;
		cmd[1] = (byte) 0x20;
		System.arraycopy(ID, 0, cmd, 2, ID.length); // UID
		cmd[10] = (byte) position;
		byte res[] = mNfcV.transceive(cmd);
		if(res[0] == 0x00){
		byte block[] = new byte[res.length - 1];
		System.arraycopy(res, 1, block, 0, res.length - 1);
		return printHexString(block);
		}
		return null;
		}
	/**
	 * 读取从begin开始end个block
	 * begin + count 不能超过blockNumber
	 * @param begin block开始位置
	 * @param count 读取block数量
	 * @return 返回内容字符串
	 * @throws IOException
	 */
	public String readBlocks(int begin, int count) throws IOException {
	if((begin + count) > blockNumber){
	count = blockNumber - begin;
	}
	StringBuffer data = new StringBuffer();
	for(int i = begin; i < count + begin; i++){
	data.append(readOneBlock(i));
	}
	return data.toString(); 
	}

	/**
	 * 将byte[]转换成16进制字符串
	 * @param data	要转换成字符串的字节数组
	 * @return 16进制字符串
	 */
	private String printHexString(byte[] data) {
	StringBuffer s = new StringBuffer();;
		for (byte datum : data) {
			String hex = Integer.toHexString(datum & 0xFF);
			if (hex.length() == 1) {
				hex = '0' + hex;
			}
			s.append(hex);
		}
	return s.toString();
	}
	/**
	 * 将数据写入到block,
	 * @param position 要写内容的block位置
	 * @param data 要写的内容,必须长度为blockOneSize
	 * @return false为写入失败，true为写入成功
	 * @throws IOException
	 */
	public boolean writeBlock(int position, byte[] data) throws IOException {
	byte cmd[] = new byte[15];
	cmd[0] = (byte) 0x22;
	cmd[1] = (byte) 0x21;
	System.arraycopy(ID, 0, cmd, 2, ID.length); // UID
	//block
	cmd[10] = (byte) position;
	//value
	System.arraycopy(data, 0, cmd, 11, data.length);
	byte[]rsp = mNfcV.transceive(cmd);
		return rsp[0] == 0x00;
	}
}
