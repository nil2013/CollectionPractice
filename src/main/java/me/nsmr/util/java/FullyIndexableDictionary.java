package me.nsmr.util.java;

public class FullyIndexableDictionary {

  private static byte[] table = null;

  private static int[] mask = {
    0x1, 0x3, 0x7, 0xf,
    0x1f, 0x3f, 0x7f, 0xff,
    0x1ff, 0x3ff, 0x7ff, 0xfff,
    0x1fff, 0x3fff, 0x7fff, 0xffff,
  };

  public static FullyIndexableDictionary buildFrom(int... binary) {
    FullyIndexableDictionary obj = new FullyIndexableDictionary(binary.length);
    int upper = 0;
    long value = 0L;
    for(int i=0; i<binary.length; i++) {
      if(upper != i >> 6) {
        obj.updateLong(upper, value);
        value = (long) binary[i];
        upper = i >> 6;
      } else {
        value = value | binary[i] << (i & 63);
      }
    }
    if(value > 0) obj.updateLong(upper, value);
    obj.refreshBuffer();
    return obj;
  }

  public static int bitCount(int binary) {
    if(table == null) {
      table = new byte[256];
      for(int i=0; i<256; i++) {
        table[i] = (byte) Integer.bitCount(i);
      }
    }
    if(binary > 255) {
      int res = bitCount(binary >> 8) + (int) table[binary & 0xff];
      return res;
    } else {
      int res = (int) table[binary];
      return res;
    }
  }

  public FullyIndexableDictionary(int size) {
    this.values = new long[(size >> 6) + 1];
    this._size = size;
  }

  public FullyIndexableDictionary(int size, long[] array) {
    this.values = array;
    this._size = size;
  }

  public int rank(int pos) {
    if(pos < 0 || pos > size()) { return -1; }
    if(this.big == null || this.small == null) { this.refreshBuffer(); }
    int b = 0;
    if(pos >> 8 > 0) {
      b = this.big[(pos >> 8) - 1];
    }
    short s = this.small[pos >> 4];
    return b + s + bitCount((int) (values[pos >> 6] >> (pos & 0x30) & mask[pos & 15]));
  }

  /**
   * Selectを二分探索で実装する
   * FIXME: 未完成
   */
  public int select(int rank) {
    System.err.println("selectはまだ正しく実装されていません");
    if(rank > size()) return -1;
    int max = size();
    int min = 0;
    int pos = -1;
    int cnt = 0;
    while(true) {
      if(cnt > 15) break; else cnt += 1;
      pos = (max + min) >> 1;
      int r = rank(pos);
      // System.out.println("pos: " + pos + ", rank: " + r + ", max: " + max + ", min: " + min);
      if(rank(pos) > rank) {
        // より大きいグループの中にいる
        min = pos + 1;
      } else if(rank(pos) < rank) {
        // より小さいグループの中にいる
        max = pos;
      } else {
        // 最小の地点を求める
        break;
      }
    }
    return pos;
  }

  public void refreshBuffer() {
    /** 大ブロック=256ビット=32バイトごと */
    int[] big = new int[(this.values.length >> 2) + 1];
    /** 小ブロック=16ビット=2バイトごと */
    short[] small = new short[this.values.length << 2];

    int _big = 0;
    for(int i=0; (i << 2) < this.values.length; i++) {
      short _small = 0;
      for(int j=0; j < 16 && ((i << 2) + (j >> 2)) < values.length; j++) {
        int idx = (i << 2) + (j >> 2);
        int shift = (j & 3) << 4;
        small[(i << 4) + j] = _small;
        _small += bitCount((int) (values[idx] >> shift & 0xffff));
      }
      if(i < big.length) { big[i] = _big; };
      _big += _small;
    }
    this.big = big;
    this.small = small;
  }

  public int apply(int pos) {
    int upper = pos >> 6;
    int lower = pos & 63;
    return (int) (values[upper] >> lower) & 1;
  }

  public int size() { return _size; }

  public void extend(int toSize) {
    int upper = toSize >> 6;
    if(upper >= values.length) {
      long[] newArray = new long[upper + 1];
      System.arraycopy(values,0,newArray,0,values.length);
      for(int i=values.length; i<newArray.length; i++) { newArray[i] = 0L; }
      this.values = newArray;
    }
    this._size = toSize;
  }

  public void update(int pos, int value) {
    if(value > 1) { return; }
    else {
      int upper = pos >> 6;
      int lower = pos & 63;
      long mask = ((0x7fffffffffffffffL << lower) << 1) | (0x7fffffffffffffffL >> (63 - lower));
      if(pos >= size()) extend(pos + 1);
      values[upper] = (values[upper] & mask) + (value << lower);
      this.big = null;
      this.small = null;
    }
  }

  public void updateLong(int pos, long value) {
    if(pos < this.values.length) this.values[pos] = value;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("FID(");
    for(int i = 0; i < values.length; i++) {
      for(int j=0; j < 64 && i * 64 + j < size(); j ++) {
        sb.append(values[i] >> j & 1);
      }
    }
    return sb.append(")").toString();
  }

  public String toBinaryString(long binary) {
    StringBuilder sb = new StringBuilder();
    for(long value : values) {
      for(int i = 0; i<64; i++) {
        sb.append(value >> i & 1);
      }
    }
    return sb.toString();
  }

  public int _size = 0;

  public long[] values = null;

  /** 大ブロック=256ビット=32バイトごと */
  public int[] big = null;

  /** 小ブロック=16ビット=2バイトごと */
  public short[] small = null;
}
