/* 
 * PROJECT: NyARToolkit JOGL sample program.
 * --------------------------------------------------------------------------------
 * The MIT License
 * Copyright (c) 2008 nyatla
 * airmail(at)ebony.plala.or.jp
 * http://nyatla.jp/nyartoolkit/
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * 
 */
package jp.nyatla.nyartoolkit.jogl.sample.artk.sketch;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import jp.nyatla.nyartoolkit.core.NyARRuntimeException;
import jp.nyatla.nyartoolkit.jmf.utils.*;
import jp.nyatla.nyartoolkit.jogl2.sketch.GlSketch;
import jp.nyatla.nyartoolkit.jogl2.utils.*;
import jp.nyatla.nyartoolkit.markersystem.NyARMarkerSystemConfig;

/**
 * BufferedImageをベースにしたImagePickupです。以下の２点が標準のImagePickupと異なります。
 * マーカパターンにPNG画像を使うことに特徴があります。また、画像取得にBufferedImageを使っています。
 */
public class ImagePickupJ2se extends GlSketch
{
	private NyARJmfCamera camera;
	private NyARGlMarkerSystem nyar;
	private NyARGlRender render;
	private final static String PNGPATT = "../../Data/hiro.png";
	public void setup(GL gl)throws Exception
	{
		try{
			this.size(640,480);
			NyARMarkerSystemConfig config = new NyARMarkerSystemConfig(640,480);
			JmfCaptureDeviceList devlist = new JmfCaptureDeviceList();
			JmfCaptureDevice d = devlist.getDevice(0);
			d.setCaptureFormat(config.getScreenSize(),30.0f);
			this.camera=new NyARJmfCamera(d);//create sensor system
			this.nyar=new NyARGlMarkerSystem(config);   //create MarkerSystem
			this.render=new NyARGlRender(this.nyar);
			//regist a marker from PNG
			this.ids[0]=this.nyar.addARMarker(ImageIO.read(new File(PNGPATT)),16,25,80);
			this._pat=new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB);
			gl.glEnable(GL.GL_DEPTH_TEST);
			//start camera
			this.camera.start();
		}catch(Exception e){
			throw new NyARRuntimeException(e);
		}
	}

	private int[] ids=new int[1];
	//temporary
	private BufferedImage _pat;
	public void draw(GL i_gl)throws Exception
	{
		//lock async update.
		synchronized(this.camera)
		{
			try{
				GL2 gl=i_gl.getGL2();
				this.nyar.update(this.camera);
				this.render.drawBackground(gl,this.camera.getSourceImage());
				gl.glPushMatrix();
				this.render.loadScreenProjectionMatrix(gl,640,480);
				this.render.setStrokeWeight(gl,1.0f);
				this.render.setColor(gl,255,255,0);
				for(int i=0;i<ids.length;i++){
					if(!this.nyar.isExist(ids[i])){
						continue;
					}
					this.render.polygon(gl,this.nyar.getVertex2D(ids[i]));					
					this.nyar.getPlaneImage(ids[i],this.camera,-40,-40,80,80,this._pat);
					this.nyar.loadTransformMatrix(gl,ids[i]);
					this.render.drawImage2d(gl,i*64,0,this._pat);
				}
				gl.glPopMatrix();
				Thread.sleep(1);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}	
	public static void main(String[] args)
	{
		new ImagePickupJ2se().run();
		return;
	}
}
