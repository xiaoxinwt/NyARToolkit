/* 
 * PROJECT: NyARToolkit
 * --------------------------------------------------------------------------------
 * This work is based on the original ARToolKit developed by
 *   Hirokazu Kato
 *   Mark Billinghurst
 *   HITLab, University of Washington, Seattle
 * http://www.hitl.washington.edu/artoolkit/
 *
 * The NyARToolkit is Java version ARToolkit class library.
 * Copyright (C)2008 R.Iizuka
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this framework; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * For further information please contact.
 *	http://nyatla.jp/nyatoolkit/
 *	<airmail(at)ebony.plala.or.jp>
 * 
 */
package jp.nyatla.nyartoolkit.sandbox.x2;

import jp.nyatla.nyartoolkit.core.types.*;
import jp.nyatla.nyartoolkit.core.param.*;
import jp.nyatla.nyartoolkit.core2.types.NyARFixedFloat16Point2d;
/**
 * 歪み成分マップを使用するINyARCameraDistortionFactor
 * 内部マップをint(1:15:16)フォーマットの固定小数点で保持する。
 * 固定小数点で値を提供するインタフェイスを持ちます。
 */
final public class NyARFixedFloatCameraDistortionFactorMap
{
	private double[] _factor=new double[4];
	private int _stride;
	private int[] _mapx;
	private int[] _mapy;
	public NyARFixedFloatCameraDistortionFactorMap(NyARCameraDistortionFactor i_distfactor,NyARIntSize i_screen_size)
	{
		NyARDoublePoint2d opoint=new NyARDoublePoint2d();
		this._mapx=new int[i_screen_size.w*i_screen_size.h];
		this._mapy=new int[i_screen_size.w*i_screen_size.h];
		this._stride=i_screen_size.w;
		int ptr=i_screen_size.h*i_screen_size.w-1;
		//歪みマップを構築
		for(int i=i_screen_size.h-1;i>=0;i--)
		{
			for(int i2=i_screen_size.w-1;i2>=0;i2--)
			{
				i_distfactor.observ2Ideal(i2,i, opoint);
				this._mapx[ptr]=(int)(opoint.x*65536);
				this._mapy[ptr]=(int)(opoint.y*65536);
				ptr--;
			}
		}
		i_distfactor.getValue(this._factor);
		return;
	}	
	public void ideal2ObservBatch(final NyARDoublePoint2d[] i_in, NyARFixedFloat16Point2d[] o_out, int i_size)
	{
		double x, y;
		final double d0 = this._factor[0];
		final double d1 = this._factor[1];
		final double d3 = this._factor[3];
		final double d2_w = this._factor[2] / 100000000.0;
		for (int i = 0; i < i_size; i++) {
			x = (i_in[i].x - d0) * d3;
			y = (i_in[i].y - d1) * d3;
			if (x == 0.0 && y == 0.0) {
				o_out[i].x = (long)(d0*NyMath.FIXEDFLOAT16_1);
				o_out[i].y = (long)(d1*NyMath.FIXEDFLOAT16_1);
			} else {
				final double d = 1.0 - d2_w * (x * x + y * y);
				o_out[i].x = (long)((x * d + d0)*NyMath.FIXEDFLOAT16_1);
				o_out[i].y = (long)((y * d + d1)*NyMath.FIXEDFLOAT16_1);
			}
		}
		return;
	}
	public void ideal2Observ(final NyARDoublePoint2d i_in, NyARFixedFloat16Point2d o_out)
	{
		final double f0=this._factor[0];
		final double f1=this._factor[1];
		final double x = (i_in.x - f0) * this._factor[3];
		final double y = (i_in.y - f1) * this._factor[3];
		if (x == 0.0 && y == 0.0) {
			o_out.x = (long)(f0*NyMath.FIXEDFLOAT16_1);
			o_out.y = (long)(f1*NyMath.FIXEDFLOAT16_1);
		} else {
			final double d = 1.0 - this._factor[2] / 100000000.0 * (x * x + y * y);
			o_out.x = (long)((x * d + f0)*NyMath.FIXEDFLOAT16_1);
			o_out.y = (long)((y * d + f1)*NyMath.FIXEDFLOAT16_1);
		}
		return;
	}	
	/**
	 * 点集合のi_start～i_numまでの間から、最大i_sample_count個の頂点を取得して返します。
	 * i_sample_countは偶数である必要があります。
	 * @param i_x_coord
	 * @param i_y_coord
	 * @param i_start
	 * @param i_num
	 * @param o_x_coord
	 * @param o_y_coord
	 * @param i_sample_count
	 * @return
	 */
	public int observ2IdealSampling(int[] i_x_coord, int[] i_y_coord,int i_start, int i_num, int[] o_x_coord,int[] o_y_coord,int i_sample_count)
	{
		assert(i_sample_count%2==0);
        int idx;
        if (i_num < i_sample_count)
        {
            for (int i = i_num - 1; i >= 0; i -= 2)
            {
                idx = i_x_coord[i_start + i] + i_y_coord[i_start + i] * this._stride;
                o_x_coord[i] = this._mapx[idx];
                o_y_coord[i] = this._mapy[idx];
            }
            return i_num;
        }
        else
        {
            //サンプリング個数分の点を、両端から半分づつ取ってくる。
            int st = i_start;
            int ed = i_start + i_num - 1;
            for (int i = i_sample_count - 1; i >= 0; i -= 2)
            {
                idx = i_x_coord[st] + i_y_coord[st] * this._stride;
                o_x_coord[i] = this._mapx[idx];
                o_y_coord[i] = this._mapy[idx];
                idx = i_x_coord[ed] + i_y_coord[ed] * this._stride;
                o_x_coord[i - 1] = this._mapx[idx];
                o_y_coord[i - 1] = this._mapy[idx];
                ed--;
                st++;
            }
            return i_sample_count;
        }
	}
}