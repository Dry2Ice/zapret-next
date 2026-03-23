import unittest

from scripts import artifact_manager


class ArtifactManagerTest(unittest.TestCase):
    def test_compatibility_major_version(self):
        self.assertTrue(artifact_manager.compatibility_ok("1.2.3", "1.0.0"))
        self.assertFalse(artifact_manager.compatibility_ok("2.0.0", "1.9.0"))

    def test_signature_roundtrip(self):
        manifest = artifact_manager.generate_manifest("1.0.0", "1.0.0")
        self.assertEqual(manifest["signature"]["value"], artifact_manager.signature_value(manifest))


if __name__ == "__main__":
    unittest.main()
