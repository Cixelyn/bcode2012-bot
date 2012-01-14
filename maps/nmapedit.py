#!/usr/bin/python

#    Copyright 2011 by Daniel Gulotta
#    This program is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with this program.  If not, see <http://www.gnu.org/licenses/>.

import curses
import traceback
from xml.dom import pulldom
from xml.sax import SAXException
from cStringIO import StringIO
import re
import sys
import random
from itertools import combinations

def make_graph(pts):
    "Returns all edges that are not the longest edge of any obtuse or right trangles."
    edges = set(combinations(pts,2))
    for p1, p2, p3 in combinations(pts,3):
        d1 = (p2[0]-p3[0])**2+(p2[1]-p3[1])**2
        d2 = (p3[0]-p1[0])**2+(p3[1]-p1[1])**2
        d3 = (p1[0]-p2[0])**2+(p1[1]-p2[1])**2
        '''
        # removes the longest edges of all triangles
        m = max(d1,d2,d3)
        if(len([d for d in (d1,d2,d3) if d==m])==1):
            if d1==m:
                edges.discard((p2,p3))
            elif d2==m:
                edges.discard((p1,p3))
            else:
                edges.discard((p1,p2))
        '''
        # removes all longest edges of right and obtuse triangles
        if d1>=d2+d3:
            edges.discard((p2,p3))
        elif d2>=d1+d3:
            edges.discard((p1,p3))
        elif d3>=d1+d2:
            edges.discard((p1,p2))
    return list(edges)

class InvalidMapFormat(IOError):
    pass

class FatalError(Exception):
    def __init__(self,code,message):
        super(FatalError,self).__init__(code,message)
        self.message = message
        self.code = code

class UsageError(FatalError):
    def __init__(self):
        super(UsageError,self).__init__(64,"usage:\nnmapedit.py filename\nnmapedit.py - height width\n")


class MapPad(object):
    
    def __init__(self,gamemap,window):
        self.gamemap = gamemap
        self.pad = curses.newpad(gamemap.height,gamemap.width)
        self.pad.scrollok(0)
        self.window = window
        self.object_keys = {}
        for i in Map.types.values():
            self.object_keys[ord(i)] = i
        self.pad.move(0,0)
        self.xoff = 0
        self.yoff = 0
        self.mode = 'draw'

    def draw(self):
        (y,x) = self.pad.getyx()
        for yy in xrange(self.gamemap.height):
            for xx in xrange(self.gamemap.width):
                try:
                    self.pad.addch(yy,xx,str(self.gamemap[yy,xx]))
                except curses.error:
                    pass
        (self.winheight, self.winwidth) = self.window.getmaxyx()
        self.winheight-=2
        self.winwidth-=1
        if x < self.xoff:
            self.xoff = x
        if x >= self.xoff + self.winwidth:
            self.xoff = x - self.winwidth
        if y < self.yoff:
            self.yoff = y
        if y > self.yoff + self.winheight:
            self.yoff = y - self.winheight
        mapheight = min(self.winheight,self.gamemap.height)
        mapwidth = min(self.winwidth, self.gamemap.width)
        self.window.addstr(mapheight,0,self.mode)
        self.pad.move(y,x)
        self.window.refresh()
        self.pad.refresh(self.yoff,self.xoff,0,0,min(self.winheight,self.gamemap.height),min(self.winwidth,self.gamemap.width))

    def setch(self,ch,y,x):
        self.gamemap[y,x]=self.object_keys[ch]

    def move(self,y,x):
        oldch = self.gamemap[self.pad.getyx()]
        try:
            self.pad.move(y,x)
        except curses.error:
            return
        if self.mode=='copy':
            self.gamemap[y,x]=oldch

    def input(self,ch):
        (y,x) = self.pad.getyx()
        if ch == curses.KEY_LEFT:
            self.move(y,x-1)
        elif ch == curses.KEY_RIGHT:
            self.move(y,x+1)
        elif ch == curses.KEY_UP:
            self.move(y-1,x)
        elif ch == curses.KEY_DOWN:
            self.move(y+1,x)
        elif ch in self.object_keys:
            self.setch(ch,y,x)
        elif ch==ord('/'):
            if self.mode=='draw':
                self.mode='copy'
            else:
                self.mode='draw'
        elif ch==ord('^'):
            self.gamemap.mirror_up()
        elif ch==27:
            raise KeyboardInterrupt

    def run(self):
        self.pad.keypad(1)
        while True:
            self.draw()
            self.input(self.pad.getch())

def get_xml_att(att,name):
    if att.has_key(name):
        return att[name].value
    else:
        return None

class Map(object):

    LAND = '.'
    VOID = '#'
    NODE = '@'
    ARCHON_A = 'A'
    ARCHON_B = 'a'
    NODE_A = 'N'
    NODE_B = 'n'

    def __init__(self, height, width, seed, rounds, objects = None):
        self.width = width
        self.height = height
        self.seed = seed
        self.rounds = rounds
        if objects is None:
            self.objects = ['.']*width*height
        else:
            self.objects = objects

    def _getindex(self,c):
        y, x = c
        if y<0 or y>=self.height:
            raise IndexError
        if x<0 or x>=self.width:
            raise IndexError
        return y*self.width+x

    def __getitem__(self,c):
        return self.objects[self._getindex(c)]

    def __setitem__(self,c,i):
        index = self._getindex(c)
        self.objects[index] = i
        self.objects[self.width*self.height-index-1] = self.opposite(i)

    types = {
        (None, 'LAND', 'TERRAIN') : LAND,
        (None, 'VOID', 'TERRAIN') : VOID,
        ('A', None, 'ARCHON') : ARCHON_A,
        ('B', None, 'ARCHON') : ARCHON_B,
        ('NEUTRAL', None, 'NODE') : NODE,
        ('A', None, 'NODE') : NODE_A,
        ('B', None, 'NODE') : NODE_B
    }

    @staticmethod
    def load(f):
        try:
            node_name = None
            tree = pulldom.parse(f)
            chars = {}
            objects = StringIO()
            for (token, node) in tree:
                if token == 'START_ELEMENT':
                    att = node.attributes
                    node_name = node.tagName
                    if node_name == 'map':
                        height = int(att['height'].value)
                        width = int(att['width'].value)
                    if node_name == 'game':
                        seed = int(att['seed'].value)
                        rounds = int(att['rounds'].value)
                    elif node_name=='symbol':
                        key = get_xml_att(att,'character')
                        tt = get_xml_att(att,'type')
                        tm = get_xml_att(att,'team')
                        terr = get_xml_att(att,'terrain')
                        try:
                            ch = Map.types[(tm,terr,tt)]
                        except KeyError:
                            pass
                        else:
                            chars[ord(key)]=ord(ch)
                elif token == 'CHARACTERS' and node_name=='data':
                    objects.write(node.nodeValue)
            translated = unicode(objects.getvalue()).translate(chars)
            for ch in translated:
                if not (ch in Map.types.values() or ch.isspace()):
                    raise FatalError(65,"Unrecognized symbol '%s' in data section"%ch)
            translated = filter(Map.types.values().__contains__,translated)
            mapobjs = list(translated)
            if len(mapobjs)!=height*width:
                raise FatalError(65,'The data section does not have the correct length.  Expected: %d got: %d' % (height*width, len(mapobjs)))
            return Map(height,width,seed,rounds,mapobjs)
        except SAXException, e:
            raise FatalError(65,"Encountered error while parsing map file:\n%s"%str(e))

    @staticmethod
    def opposite(ch):
        if ch.isupper():
            return ch.lower()
        elif ch.islower():
            return ch.upper()
        else:
            return ch

    def mirror_up(self):
        for y in xrange(self.height/2,self.height):
            for x in xrange(self.width):
                self[y,x] = self[y,x]        

    def __str__(self):
        return '\n'.join(''.join(self.objects[i:i+self.width]) for i in xrange(0,self.height*self.width, self.width))

    def to_xml(self):
        height = self.height
        width = self.width
        seed = self.seed
        rounds = self.rounds
        map_str = str(self)
        map_heights = ('G'*width+'\n')*height
        nodes = [(x,y) for y in xrange(self.height) for x in xrange(self.width) if self[y,x] in '@Nn']
        links = make_graph(nodes)
        nodelinks='\n\t'.join('<nodelink from="%d,%d" to="%d,%d"/>'%(p1[0],p1[1],p2[0],p2[1]) for p1,p2 in links)
        return '''<?xml version="1.0" encoding="UTF-8"?>
<map height="%(height)d" width="%(width)d">
    <game seed="%(seed)d" rounds="%(rounds)d"/>
    <symbols>
        <symbol terrain="LAND" type="TERRAIN" character="."/>
        <symbol terrain="VOID" type="TERRAIN" character="#"/>
        <symbol team="NEUTRAL" type="NODE" character="@"/>
        <symbol team="A" type="NODE" character="N"/>
        <symbol team="B" type="NODE" character="n"/>
        <symbol team="A" type="ARCHON" character="A"/>
        <symbol team="B" type="ARCHON" character="a"/>
    </symbols>
    <nodelinks>
        %(nodelinks)s
    </nodelinks>
    <data>
<![CDATA[
%(map_str)s
]]>
    </data>
    <height>
<![CDATA[
%(map_heights)s
]]>
    </height>
</map>
''' % locals()

def usage():
    sys.stderr.write("usage:\n")
    sys.stderr.write("nmapedit.py filename\n")
    sys.stderr.write("nmapedit.py - height width\n")

class MapEditor(object):

    def mainloop(self,stdscr,filename='-',y=None,x=None):
        self.scr = stdscr
        if filename!='-':
            try:
                self.load(filename)
            except IOError, e:
                raise FatalError(66,"%s: %s"%(e.filename,e.strerror))
        else:
            try:
                ix = int(x)
                iy = int(y)
            except TypeError:
                raise UsageError
            if ix<=0 or iy<=0:
                raise UsageError
            self.new(int(y),int(x))

    def set_map(self,newmap):
        self.gamemap = newmap
        pad = MapPad(newmap,self.scr)
        pad.run()

    def load(self,filename):
        self.set_map(Map.load(filename))

    def new(self,x,y):
        self.set_map(Map(y,x,random.randint(0,0x7FFFFFFF),7000))

    def savePrompt(self):
        prompt = 'Save changes? '
        while True:
            svo = raw_input(prompt)
            sv = svo.upper()
            if sv=='Y' or sv=='YES':
                name = raw_input('Filename (default %s): '%sys.argv[1])
                if not name:
                    name = sys.argv[1]
                try:
                    f = open(name,'w')
                    f.write(self.gamemap.to_xml())
                    f.close()
                    return
                except IOError, OSError:
                    prompt = 'Save failed.  Try again? '
            elif sv=='N' or sv=='NO':
                return
            else:
                print '''Sorry, response '%s' not understood'''%svo

if __name__ == '__main__':
    ed = MapEditor()
    try:
        curses.wrapper(ed.mainloop,*sys.argv[1:])
    except FatalError, e:
        sys.stderr.write(e.message)
        sys.stderr.write("\n")
        sys.exit(e.code)
    except KeyboardInterrupt:
        ed.savePrompt()
    except:
        traceback.print_exc(file=sys.stderr)
        sys.exit(70)
