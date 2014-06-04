import sys

from pymol import cmd


def save_surface(arg1):
    cmd.disable()
    cmd.enable(arg1)
    cmd.hide("everything", arg1)
    cmd.show("surface", arg1)
    cmd.save("%s.obj" % arg1, arg1)
    cmd.enable()


def prepare_data(arg1):
    cmd.load("%s_r_b.pdb" % arg1, zoom=0)
    cmd.load("%s_l_b.pdb" % arg1, zoom=0)
    cmd.load("%s_r_u.pdb" % arg1, zoom=0)
    cmd.load("%s_l_u.pdb" % arg1, zoom=0)

    save_surface("%s_r_b" % arg1)
    save_surface("%s_l_b" % arg1)
    save_surface("%s_r_u" % arg1)
    save_surface("%s_l_u" % arg1)

    cmd.align("%s_r_b" % arg1, "%s_r_u" % arg1, object="ra")
    cmd.align("%s_l_b" % arg1, "%s_l_u" % arg1, object="la")
    cmd.save("receptor.aln", "ra")
    cmd.save("ligand.aln", "la")


print "Running pymol for complex: %s" % sys.argv[1]
prepare_data(sys.argv[1])
