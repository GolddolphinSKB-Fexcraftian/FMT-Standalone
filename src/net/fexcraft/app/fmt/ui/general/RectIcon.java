package net.fexcraft.app.fmt.ui.general;

import net.fexcraft.app.fmt.ui.Element;
import net.fexcraft.lib.common.math.RGB;

/**
 * 
 * @author Ferdinand Calo' (FEX___96)
 *
 */
public class RectIcon extends Element {
	
	public RectIcon(Element root, String id, String style, String texture, int sizex, int sizey, int x, int y){
		super(root, id, style); this.setPosition(x, y).setSize(sizex, sizey).setTexture(texture, true).setEnabled(true);
	}
	
	public RectIcon(Element elm, String id, String style, String texture, int sizex, int sizey, int x, int y, RGB hover){
		this(elm, id, style, texture, sizex, sizey, x, y); hovercolor = hover;
	}
	
	public RectIcon(Element elm, String id, String style, String texture, int sizex, int sizey, int x, int y, RGB hover, RGB dis){
		this(elm, id, style, texture, sizex, sizey, x, y, hover); discolor = dis;
	}

	@Override
	public void renderSelf(int rw, int rh){
		if(hovered) (enabled ? hovercolor : discolor).glColorApply();
		this.renderQuad(x, y, width, height, texture);
		if(hovered) RGB.glColorReset();
	}
	
	@Override
	public void hovered(float mx, float my){
		super.hovered(mx, my); if(this.hovered){ for(Element elm : elements) elm.setVisible(true); }
	}

}
