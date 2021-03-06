package net.fexcraft.app.fmt.ui.tree;

import org.lwjgl.input.Mouse;

import net.fexcraft.app.fmt.utils.HelperCollector;
import net.fexcraft.app.fmt.wrappers.GroupCompound;
import net.fexcraft.app.fmt.wrappers.TurboList;

public class HelperTree extends RightTree {

	public static HelperTree TREE = new HelperTree();
	public static int SEL = -1;
	private int elm_height;

	private HelperTree(){ super("helpertree"); TREE = this; }

	@Override
	public void renderSelf(int rw, int rh){
		elm_height = 4; elements.clear(); if(HelperCollector.LOADED.size() == 0) SEL = -1; elm_height -= scroll; boolean bool;
		for(GroupCompound compound : HelperCollector.LOADED){
			if((bool = elm_height < 4) && compound.minimized){ elm_height += 28; continue; } if(elm_height > height) break;
			if(!bool){ compound.button.update(elm_height, rw, rh); elm_height += 28; elements.add(compound.button); }
			if(compound.minimized) continue;
			for(TurboList list : compound.getGroups()){
				if(elm_height < 4){ elm_height += 28; continue; } if(elm_height > height) break;
				list.button.update(elm_height, rw, rh); elm_height += 28; elements.add(list.button);
			}
		}
	}

	@Override
	public void hovered(float mx, float my){
		super.hovered(mx, my);
	}

	public boolean processScrollWheel(int wheel){
		this.modifyScroll(-wheel / (Mouse.isButtonDown(1) ? 1 : 10)); return true;
	}
	
	public void modifyScroll(int amount){
		scroll += amount; if(scroll < 0) scroll = 0;
	}

	public static GroupCompound getSelected(){
		return SEL >= HelperCollector.LOADED.size() || SEL < 0 ? null : HelperCollector.LOADED.get(SEL);
	}
	
}
